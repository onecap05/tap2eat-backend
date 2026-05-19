package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.embedded.ModifierOption;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
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

    @Override
    public List<RestaurantDocument> getActiveRestaurants() {
        return restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull();
    }

    @Override
    public RestaurantDocument getActiveRestaurantById(String restaurantId) {
        validateId(restaurantId);

        return restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<BranchDocument> getActiveBranchesByRestaurant(String restaurantId) {
        getActiveRestaurantById(restaurantId);

        return branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId);
    }

    @Override
    public List<CategoryDocument> getActiveCategoriesByRestaurant(String restaurantId) {
        getActiveRestaurantById(restaurantId);

        return categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId).stream()
                .sorted(Comparator.comparing(
                        category -> category.getDisplayOrder() == null ? Integer.MAX_VALUE : category.getDisplayOrder()
                ))
                .toList();
    }

    @Override
    public List<ProductDocument> getAvailableProductsByRestaurant(String restaurantId) {
        getActiveRestaurantById(restaurantId);
        Set<String> activeCategoryIds = getActiveCategoryIds(restaurantId);

        return productRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId).stream()
                .filter(product -> activeCategoryIds.contains(product.getCategoryId()))
                .filter(this::isAvailableProduct)
                .sorted(Comparator.comparing(
                        product -> product.getDisplayOrder() == null ? Integer.MAX_VALUE : product.getDisplayOrder()
                ))
                .toList();
    }

    @Override
    public ProductDocument getAvailableProductById(String productId) {
        validateId(productId);

        ProductDocument product = productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(productId)
                .filter(this::isAvailableProduct)
                .map(this::filterInactiveModifiers)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));

        getActiveRestaurantById(product.getRestaurantId());
        validateProductCategoryIsActive(product);

        return product;
    }

    private Set<String> getActiveCategoryIds(String restaurantId) {
        return categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId).stream()
                .map(CategoryDocument::getId)
                .collect(Collectors.toSet());
    }

    private void validateProductCategoryIsActive(ProductDocument product) {
        if (!getActiveCategoryIds(product.getRestaurantId()).contains(product.getCategoryId())) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private boolean isAvailableProduct(ProductDocument product) {
        if (product == null || Boolean.FALSE.equals(product.getIsActive()) || product.getDeletedAt() != null) {
            return false;
        }

        AvailabilityConfig availability = product.getAvailability();

        return availability == null || AvailabilityStatus.AVAILABLE.equals(availability.getStatus());
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
