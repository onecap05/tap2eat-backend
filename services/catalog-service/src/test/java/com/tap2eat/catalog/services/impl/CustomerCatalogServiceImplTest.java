package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCatalogServiceImplTest {

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private IBranchRepository branchRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private IProductRepository productRepository;

    @InjectMocks
    private CustomerCatalogServiceImpl customerCatalogService;

    @Test
    void getActiveRestaurants_shouldReturnOnlyRepositoryActiveRestaurants() {
        RestaurantDocument restaurant = restaurant("restaurant-1");
        when(restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull()).thenReturn(List.of(restaurant));

        List<RestaurantDocument> restaurants = customerCatalogService.getActiveRestaurants();

        assertThat(restaurants).containsExactly(restaurant);
    }

    @Test
    void getActiveRestaurantById_shouldFailWhenRestaurantIsInactiveOrMissing() {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerCatalogService.getActiveRestaurantById("restaurant-1"))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getAvailableProductsByRestaurant_shouldReturnOnlyAvailableProductsInActiveCategories() {
        CategoryDocument category = category("category-1", "restaurant-1");
        ProductDocument availableProduct = product("product-1", "restaurant-1", "category-1", AvailabilityStatus.AVAILABLE);
        ProductDocument pausedProduct = product("product-2", "restaurant-1", "category-1", AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        ProductDocument inactiveCategoryProduct = product("product-3", "restaurant-1", "category-2", AvailabilityStatus.AVAILABLE);

        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(Optional.of(restaurant("restaurant-1")));
        when(categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(category));
        when(productRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(availableProduct, pausedProduct, inactiveCategoryProduct));

        List<ProductDocument> products = customerCatalogService.getAvailableProductsByRestaurant("restaurant-1");

        assertThat(products).containsExactly(availableProduct);
    }

    @Test
    void getAvailableProductById_shouldFailWhenProductIsPaused() {
        ProductDocument product = product("product-1", "restaurant-1", "category-1", AvailabilityStatus.PERMANENTLY_UNAVAILABLE);

        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("product-1"))
                .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> customerCatalogService.getAvailableProductById("product-1"))
                .isInstanceOf(CatalogValidationException.class);
    }

    private RestaurantDocument restaurant(String id) {
        RestaurantDocument restaurant = new RestaurantDocument();
        restaurant.setId(id);
        restaurant.setName("Demo");
        restaurant.setIsActive(Boolean.TRUE);
        return restaurant;
    }

    private CategoryDocument category(String id, String restaurantId) {
        CategoryDocument category = new CategoryDocument();
        category.setId(id);
        category.setRestaurantId(restaurantId);
        category.setName("Category");
        category.setIsActive(Boolean.TRUE);
        return category;
    }

    private ProductDocument product(
            String id,
            String restaurantId,
            String categoryId,
            AvailabilityStatus availabilityStatus
    ) {
        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(availabilityStatus);

        ProductDocument product = new ProductDocument();
        product.setId(id);
        product.setRestaurantId(restaurantId);
        product.setCategoryId(categoryId);
        product.setName("Product");
        product.setPrice(BigDecimal.TEN);
        product.setAvailability(availability);
        product.setIsActive(Boolean.TRUE);
        return product;
    }
}
