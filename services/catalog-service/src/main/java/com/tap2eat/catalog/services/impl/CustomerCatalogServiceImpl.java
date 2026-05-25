package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.dtos.response.customer.CustomerBranchResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerCategoryResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerProductResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerRestaurantResponse;
import com.tap2eat.catalog.mappers.CustomerCatalogResponseMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.embedded.ModifierOption;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.IAvailabilityEvaluator;
import com.tap2eat.catalog.services.ICustomerCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCatalogServiceImpl implements ICustomerCatalogService {

    private static final Map<String, String> SEARCH_ALIASES = Map.of(
            "tacoz", "tacos",
            "taco", "tacos",
            "burger", "hamburguesa",
            "hamburgesa", "hamburguesa",
            "hamburguesas", "hamburguesa",
            "postre", "postres",
            "bebida", "bebidas"
    );

    private final IRestaurantRepository restaurantRepository;
    private final IBranchRepository branchRepository;
    private final ICategoryRepository categoryRepository;
    private final IProductRepository productRepository;
    private final IAvailabilityEvaluator availabilityEvaluator;
    private final CustomerCatalogResponseMapper customerCatalogResponseMapper;

    @Override
    public List<CustomerRestaurantResponse> getActiveRestaurants() {
        return restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull().stream()
                .map(restaurant -> customerCatalogResponseMapper.toRestaurantResponse(
                        restaurant,
                        hasOpenBranch(restaurant.getId())
                ))
                .toList();
    }

    @Override
    public List<CustomerRestaurantResponse> searchActiveRestaurants(String query) {
        String normalizedQuery = normalizeSearchText(query);

        if (normalizedQuery.isBlank()) {
            return getActiveRestaurants();
        }

        String aliasedQuery = SEARCH_ALIASES.getOrDefault(normalizedQuery, normalizedQuery);
        List<RestaurantDocument> activeRestaurants = restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull();
        Map<String, RestaurantDocument> activeRestaurantsById = activeRestaurants.stream()
                .collect(Collectors.toMap(
                        RestaurantDocument::getId,
                        restaurant -> restaurant,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        Map<String, RestaurantDocument> matches = new LinkedHashMap<>();

        for (RestaurantDocument restaurant : activeRestaurants) {
            if (matchesRestaurant(restaurant, aliasedQuery)) {
                matches.put(restaurant.getId(), restaurant);
            }
        }

        for (ProductDocument product : productRepository.findAllByIsActiveTrueAndDeletedAtIsNull()) {
            RestaurantDocument restaurant = activeRestaurantsById.get(product.getRestaurantId());

            if (restaurant != null && matchesProduct(product, aliasedQuery)) {
                matches.putIfAbsent(restaurant.getId(), restaurant);
            }
        }

        return matches.values().stream()
                .map(restaurant -> customerCatalogResponseMapper.toRestaurantResponse(
                        restaurant,
                        hasOpenBranch(restaurant.getId())
                ))
                .toList();
    }

    @Override
    public CustomerRestaurantResponse getActiveRestaurantById(String restaurantId) {
        RestaurantDocument restaurant = getActiveRestaurantOrThrow(restaurantId);

        return customerCatalogResponseMapper.toRestaurantResponse(restaurant, hasOpenBranch(restaurantId));
    }

    @Override
    public List<CustomerBranchResponse> getActiveBranchesByRestaurant(String restaurantId) {
        getActiveRestaurantOrThrow(restaurantId);

        return getVisibleBranches(restaurantId).stream()
                .map(branch -> customerCatalogResponseMapper.toBranchResponse(
                        branch,
                        availabilityEvaluator.isBranchOpen(branch)
                ))
                .toList();
    }

    @Override
    public List<CustomerCategoryResponse> getAvailableCategoriesByRestaurant(String restaurantId) {
        getActiveRestaurantOrThrow(restaurantId);

        if (!hasOpenBranch(restaurantId)) {
            return List.of();
        }

        return getAvailableCategories(restaurantId).stream()
                .sorted(Comparator.comparing(
                        category -> category.getDisplayOrder() == null ? Integer.MAX_VALUE : category.getDisplayOrder()
                ))
                .map(category -> customerCatalogResponseMapper.toCategoryResponse(category, true))
                .toList();
    }

    @Override
    public List<CustomerProductResponse> getAvailableProductsByRestaurant(String restaurantId) {
        getActiveRestaurantOrThrow(restaurantId);

        if (!hasOpenBranch(restaurantId)) {
            return List.of();
        }

        Set<String> availableCategoryIds = getAvailableCategoryIds(restaurantId);

        return productRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId).stream()
                .filter(product -> availableCategoryIds.contains(product.getCategoryId()))
                .filter(availabilityEvaluator::isProductAvailable)
                .map(this::filterInactiveModifiers)
                .sorted(Comparator.comparing(
                        product -> product.getDisplayOrder() == null ? Integer.MAX_VALUE : product.getDisplayOrder()
                ))
                .map(product -> customerCatalogResponseMapper.toProductResponse(product, true))
                .toList();
    }

    @Override
    public CustomerProductResponse getAvailableProductById(String productId) {
        validateId(productId);

        ProductDocument product = productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(productId)
                .filter(availabilityEvaluator::isProductAvailable)
                .map(this::filterInactiveModifiers)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));

        getActiveRestaurantOrThrow(product.getRestaurantId());
        validateRestaurantIsOpen(product.getRestaurantId());
        validateProductCategoryIsAvailable(product);

        return customerCatalogResponseMapper.toProductResponse(product, true);
    }

    private RestaurantDocument getActiveRestaurantOrThrow(String restaurantId) {
        validateId(restaurantId);

        return restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    private boolean hasOpenBranch(String restaurantId) {
        return getVisibleBranches(restaurantId).stream().anyMatch(availabilityEvaluator::isBranchOpen);
    }

    private List<BranchDocument> getVisibleBranches(String restaurantId) {
        return branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId);
    }

    private List<CategoryDocument> getAvailableCategories(String restaurantId) {
        return categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId).stream()
                .filter(availabilityEvaluator::isCategoryAvailable)
                .toList();
    }

    private Set<String> getAvailableCategoryIds(String restaurantId) {
        return getAvailableCategories(restaurantId).stream()
                .map(CategoryDocument::getId)
                .collect(Collectors.toSet());
    }

    private void validateProductCategoryIsAvailable(ProductDocument product) {
        if (!getAvailableCategoryIds(product.getRestaurantId()).contains(product.getCategoryId())) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private void validateRestaurantIsOpen(String restaurantId) {
        if (!hasOpenBranch(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private ProductDocument filterInactiveModifiers(ProductDocument product) {
        if (product.getModifierGroups() == null) {
            return product;
        }

        List<ModifierGroup> activeGroups = product.getModifierGroups().stream()
                .filter(group -> group != null && Boolean.TRUE.equals(group.getIsActive()))
                .peek(group -> group.setOptions(activeOptions(group.getOptions())))
                .toList();

        product.setModifierGroups(activeGroups);

        return product;
    }

    private List<ModifierOption> activeOptions(List<ModifierOption> options) {
        if (options == null) {
            return List.of();
        }

        return options.stream()
                .filter(option -> option != null && Boolean.TRUE.equals(option.getIsActive()))
                .toList();
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private boolean matchesRestaurant(RestaurantDocument restaurant, String normalizedQuery) {
        return containsSearchText(restaurant.getName(), normalizedQuery)
                || containsSearchText(restaurant.getDescription(), normalizedQuery);
    }

    private boolean matchesProduct(ProductDocument product, String normalizedQuery) {
        return containsSearchText(product.getName(), normalizedQuery)
                || containsSearchText(product.getDescription(), normalizedQuery)
                || (product.getTags() != null && product.getTags().stream().anyMatch(tag -> containsSearchText(tag, normalizedQuery)));
    }

    private boolean containsSearchText(String value, String normalizedQuery) {
        return normalizeSearchText(value)
                .replace('-', ' ')
                .contains(normalizedQuery.replace('-', ' '));
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", " ");
    }
}
