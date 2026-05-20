package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.response.customer.CustomerProductResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerRestaurantResponse;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.mappers.CustomerCatalogResponseMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.IAvailabilityEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCatalogServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-19T18:00:00Z"),
            ZoneId.of("UTC")
    );

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private IBranchRepository branchRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private IProductRepository productRepository;

    private CustomerCatalogServiceImpl customerCatalogService;

    @BeforeEach
    void setUp() {
        IAvailabilityEvaluator availabilityEvaluator = new AvailabilityEvaluatorImpl(FIXED_CLOCK);
        customerCatalogService = new CustomerCatalogServiceImpl(
                restaurantRepository,
                branchRepository,
                categoryRepository,
                productRepository,
                availabilityEvaluator,
                new CustomerCatalogResponseMapper()
        );
    }

    @Test
    void getActiveRestaurants_shouldMarkRestaurantOpenWhenAtLeastOneBranchIsOpen() {
        RestaurantDocument restaurant = restaurant("restaurant-1");
        when(restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull()).thenReturn(List.of(restaurant));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(branch("branch-1", "restaurant-1", true)));

        List<CustomerRestaurantResponse> restaurants = customerCatalogService.getActiveRestaurants();

        assertThat(restaurants).hasSize(1);
        assertThat(restaurants.getFirst().open()).isTrue();
    }

    @Test
    void getActiveRestaurants_shouldMarkRestaurantClosedWhenNoBranchIsOpen() {
        RestaurantDocument restaurant = restaurant("restaurant-1");
        when(restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull()).thenReturn(List.of(restaurant));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(branch("branch-1", "restaurant-1", false)));

        List<CustomerRestaurantResponse> restaurants = customerCatalogService.getActiveRestaurants();

        assertThat(restaurants.getFirst().open()).isFalse();
    }

    @Test
    void getActiveRestaurantById_shouldFailWhenRestaurantIsInactiveOrMissing() {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerCatalogService.getActiveRestaurantById("restaurant-1"))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getAvailableProductsByRestaurant_shouldReturnOnlyAvailableProductsInAvailableCategoriesWhenRestaurantIsOpen() {
        CategoryDocument category = category("category-1", "restaurant-1", true);
        ProductDocument availableProduct = product("product-1", "restaurant-1", "category-1", AvailabilityStatus.AVAILABLE, true);
        ProductDocument pausedProduct = product("product-2", "restaurant-1", "category-1", AvailabilityStatus.TEMPORARILY_UNAVAILABLE, true);
        ProductDocument inactiveCategoryProduct = product("product-3", "restaurant-1", "category-2", AvailabilityStatus.AVAILABLE, true);

        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(Optional.of(restaurant("restaurant-1")));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(branch("branch-1", "restaurant-1", true)));
        when(categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(category));
        when(productRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(availableProduct, pausedProduct, inactiveCategoryProduct));

        List<CustomerProductResponse> products = customerCatalogService.getAvailableProductsByRestaurant("restaurant-1");

        assertThat(products).extracting(CustomerProductResponse::id).containsExactly("product-1");
        assertThat(products.getFirst().available()).isTrue();
    }

    @Test
    void getAvailableProductsByRestaurant_shouldReturnEmptyWhenRestaurantIsClosed() {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(Optional.of(restaurant("restaurant-1")));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(branch("branch-1", "restaurant-1", false)));

        List<CustomerProductResponse> products = customerCatalogService.getAvailableProductsByRestaurant("restaurant-1");

        assertThat(products).isEmpty();
    }

    @Test
    void getAvailableProductById_shouldFailWhenProductIsPaused() {
        ProductDocument product = product(
                "product-1",
                "restaurant-1",
                "category-1",
                AvailabilityStatus.PERMANENTLY_UNAVAILABLE,
                true
        );

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

    private BranchDocument branch(String id, String restaurantId, boolean openNow) {
        BranchDocument branch = new BranchDocument();
        branch.setId(id);
        branch.setRestaurantId(restaurantId);
        branch.setName("Branch");
        branch.setIsActive(Boolean.TRUE);
        branch.setAvailability(availability(openNow, true));
        return branch;
    }

    private CategoryDocument category(String id, String restaurantId, boolean availableNow) {
        CategoryDocument category = new CategoryDocument();
        category.setId(id);
        category.setRestaurantId(restaurantId);
        category.setName("Category");
        category.setIsActive(Boolean.TRUE);
        category.setAvailability(availability(availableNow, true));
        return category;
    }

    private ProductDocument product(
            String id,
            String restaurantId,
            String categoryId,
            AvailabilityStatus availabilityStatus,
            boolean inSchedule
    ) {
        ProductDocument product = new ProductDocument();
        product.setId(id);
        product.setRestaurantId(restaurantId);
        product.setCategoryId(categoryId);
        product.setName("Product");
        product.setPrice(BigDecimal.TEN);
        product.setAvailability(availability(inSchedule, true));
        product.getAvailability().setStatus(availabilityStatus);
        product.setIsActive(Boolean.TRUE);
        return product;
    }

    private AvailabilityConfig availability(boolean inSchedule, boolean enabled) {
        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(AvailabilityStatus.AVAILABLE);
        availability.setWeeklySchedule(List.of(new DailyAvailability(
                DayOfWeek.TUESDAY,
                enabled,
                List.of(new TimeRange(
                        inSchedule ? LocalTime.of(17, 0) : LocalTime.of(8, 0),
                        inSchedule ? LocalTime.of(19, 0) : LocalTime.of(10, 0)
                ))
        )));
        return availability;
    }
}
