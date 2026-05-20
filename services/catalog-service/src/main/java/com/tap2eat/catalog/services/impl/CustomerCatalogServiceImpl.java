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

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerCatalogServiceImpl implements ICustomerCatalogService {

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
}
