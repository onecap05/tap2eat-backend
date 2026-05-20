package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.response.customer.CustomerCategoryResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerProductResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerRestaurantResponse;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.CustomerCatalogResponseMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.ProductType;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCatalogServiceImplTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-20T00:00:00Z"),
            ZoneId.of("America/Mexico_City")
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
    void getActiveRestaurants_shouldListActiveRestaurantsAndMarkOpenStates() {
        RestaurantDocument openRestaurant = restaurant("restaurant-open");
        RestaurantDocument noScheduleRestaurant = restaurant("restaurant-no-schedule");
        RestaurantDocument closedRestaurant = restaurant("restaurant-closed");
        when(restaurantRepository.findAllByIsActiveTrueAndDeletedAtIsNull())
                .thenReturn(List.of(openRestaurant, noScheduleRestaurant, closedRestaurant));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-open"))
                .thenReturn(List.of(branch("branch-open", "restaurant-open", CatalogTestDataFactory.openAvailability())));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-no-schedule"))
                .thenReturn(List.of(branch("branch-no-schedule", "restaurant-no-schedule", null)));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-closed"))
                .thenReturn(List.of(branch("branch-closed", "restaurant-closed", CatalogTestDataFactory.closedAvailability())));

        List<CustomerRestaurantResponse> restaurants = customerCatalogService.getActiveRestaurants();

        assertThat(restaurants).extracting(CustomerRestaurantResponse::id)
                .containsExactly("restaurant-open", "restaurant-no-schedule", "restaurant-closed");
        assertThat(restaurants).extracting(CustomerRestaurantResponse::open)
                .containsExactly(true, true, false);
    }

    @Test
    void getActiveRestaurantById_shouldReturnActiveRestaurantWithOpenState() {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(Optional.of(restaurant("restaurant-1")));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(branch("branch-1", "restaurant-1", CatalogTestDataFactory.openAvailability())));

        CustomerRestaurantResponse response = customerCatalogService.getActiveRestaurantById("restaurant-1");

        assertThat(response.id()).isEqualTo("restaurant-1");
        assertThat(response.open()).isTrue();
    }

    @Test
    void getActiveRestaurantById_shouldFailWhenRestaurantDoesNotExistOrIsDeleted() {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-deleted"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerCatalogService.getActiveRestaurantById("restaurant-deleted"))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> customerCatalogService.getActiveRestaurantById(" "))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getAvailableCategoriesByRestaurant_shouldIncludeOnlyAvailableCategoriesWhenRestaurantIsOpen() {
        whenRestaurantIsOpen("restaurant-1");
        CategoryDocument active = category("category-active", "restaurant-1", CatalogTestDataFactory.openAvailability());
        CategoryDocument deleted = category("category-deleted", "restaurant-1", CatalogTestDataFactory.openAvailability());
        deleted.setDeletedAt(LocalDateTime.now());
        CategoryDocument outsideSchedule = category("category-closed", "restaurant-1", CatalogTestDataFactory.closedAvailability());
        when(categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(active, deleted, outsideSchedule));

        List<CustomerCategoryResponse> categories = customerCatalogService.getAvailableCategoriesByRestaurant("restaurant-1");

        assertThat(categories).extracting(CustomerCategoryResponse::id).containsExactly("category-active");
        assertThat(categories.getFirst().available()).isTrue();
    }

    @Test
    void getAvailableCategoriesByRestaurant_shouldReturnEmptyWhenRestaurantIsClosed() {
        whenRestaurantIsClosed("restaurant-1");

        List<CustomerCategoryResponse> categories = customerCatalogService.getAvailableCategoriesByRestaurant("restaurant-1");

        assertThat(categories).isEmpty();
    }

    @Test
    void getAvailableProductsByRestaurant_shouldIncludeOnlyActiveAvailableProductsInAvailableCategories() {
        whenRestaurantIsOpen("restaurant-1");
        when(categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(category("category-1", "restaurant-1", CatalogTestDataFactory.openAvailability())));
        ProductDocument simple = CatalogTestDataFactory.simpleProduct("product-simple", "restaurant-1", "category-1");
        ProductDocument customizable = customizableProduct("product-customizable", "restaurant-1", "category-1");
        ProductDocument deleted = CatalogTestDataFactory.simpleProduct("product-deleted", "restaurant-1", "category-1");
        deleted.setDeletedAt(LocalDateTime.now());
        ProductDocument paused = CatalogTestDataFactory.simpleProduct("product-paused", "restaurant-1", "category-1");
        paused.getAvailability().setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        ProductDocument differentCategory = CatalogTestDataFactory.simpleProduct("product-other", "restaurant-1", "category-2");
        when(productRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(simple, customizable, deleted, paused, differentCategory));

        List<CustomerProductResponse> products = customerCatalogService.getAvailableProductsByRestaurant("restaurant-1");

        assertThat(products).extracting(CustomerProductResponse::id)
                .containsExactly("product-simple", "product-customizable");
        assertThat(products.get(0).productType()).isEqualTo(ProductType.SIMPLE);
        assertThat(products.get(1).productType()).isEqualTo(ProductType.CUSTOMIZABLE);
        assertThat(products.get(1).modifierGroups()).hasSize(1);
        assertThat(products.get(1).modifierGroups().getFirst().options()).hasSize(1);
    }

    @Test
    void getAvailableProductsByRestaurant_shouldReturnEmptyWhenRestaurantIsClosed() {
        whenRestaurantIsClosed("restaurant-1");

        List<CustomerProductResponse> products = customerCatalogService.getAvailableProductsByRestaurant("restaurant-1");

        assertThat(products).isEmpty();
    }

    @Test
    void getAvailableProductById_shouldReturnProductWhenRestaurantAndCategoryAreAvailable() {
        ProductDocument product = customizableProduct("product-1", "restaurant-1", "category-1");
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("product-1"))
                .thenReturn(Optional.of(product));
        whenRestaurantIsOpen("restaurant-1");
        when(categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-1"))
                .thenReturn(List.of(category("category-1", "restaurant-1", CatalogTestDataFactory.openAvailability())));

        CustomerProductResponse response = customerCatalogService.getAvailableProductById("product-1");

        assertThat(response.id()).isEqualTo("product-1");
        assertThat(response.available()).isTrue();
        assertThat(response.modifierGroups()).hasSize(1);
    }

    @Test
    void getAvailableProductById_shouldFailForPausedProductClosedRestaurantOrUnavailableCategory() {
        ProductDocument paused = CatalogTestDataFactory.simpleProduct("product-paused", "restaurant-1", "category-1");
        paused.getAvailability().setStatus(AvailabilityStatus.PERMANENTLY_UNAVAILABLE);
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("product-paused"))
                .thenReturn(Optional.of(paused));

        assertThatThrownBy(() -> customerCatalogService.getAvailableProductById("product-paused"))
                .isInstanceOf(CatalogValidationException.class);

        ProductDocument product = CatalogTestDataFactory.simpleProduct("product-closed", "restaurant-2", "category-1");
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("product-closed"))
                .thenReturn(Optional.of(product));
        whenRestaurantIsClosed("restaurant-2");

        assertThatThrownBy(() -> customerCatalogService.getAvailableProductById("product-closed"))
                .isInstanceOf(CatalogValidationException.class);

        ProductDocument unavailableCategoryProduct = CatalogTestDataFactory.simpleProduct(
                "product-category",
                "restaurant-3",
                "category-closed"
        );
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull("product-category"))
                .thenReturn(Optional.of(unavailableCategoryProduct));
        whenRestaurantIsOpen("restaurant-3");
        when(categoryRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull("restaurant-3"))
                .thenReturn(List.of(category("category-closed", "restaurant-3", CatalogTestDataFactory.closedAvailability())));

        assertThatThrownBy(() -> customerCatalogService.getAvailableProductById("product-category"))
                .isInstanceOf(CatalogValidationException.class);
    }

    private void whenRestaurantIsOpen(String restaurantId) {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId))
                .thenReturn(Optional.of(restaurant(restaurantId)));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId))
                .thenReturn(List.of(branch("branch-" + restaurantId, restaurantId, CatalogTestDataFactory.openAvailability())));
    }

    private void whenRestaurantIsClosed(String restaurantId) {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId))
                .thenReturn(Optional.of(restaurant(restaurantId)));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId))
                .thenReturn(List.of(branch("branch-" + restaurantId, restaurantId, CatalogTestDataFactory.closedAvailability())));
    }

    private RestaurantDocument restaurant(String id) {
        return CatalogTestDataFactory.restaurant(id, "owner-" + id);
    }

    private BranchDocument branch(String id, String restaurantId, AvailabilityConfig availability) {
        return CatalogTestDataFactory.branch(id, restaurantId, availability);
    }

    private CategoryDocument category(String id, String restaurantId, AvailabilityConfig availability) {
        CategoryDocument category = CatalogTestDataFactory.category(id, restaurantId, availability);
        category.setDisplayOrder(id.endsWith("active") ? 1 : 2);
        return category;
    }

    private ProductDocument customizableProduct(String id, String restaurantId, String categoryId) {
        ProductDocument product = CatalogTestDataFactory.simpleProduct(id, restaurantId, categoryId);
        product.setProductType(ProductType.CUSTOMIZABLE);
        List<ModifierGroup> groups = new ArrayList<>();
        ModifierGroup activeGroup = CatalogTestDataFactory.modifierGroup();
        activeGroup.setOptions(List.of(
                CatalogTestDataFactory.modifierOption("active-option", "Verde", true),
                CatalogTestDataFactory.modifierOption("inactive-option", "Roja", false)
        ));
        groups.add(activeGroup);
        ModifierGroup inactiveGroup = CatalogTestDataFactory.modifierGroup();
        inactiveGroup.setId("inactive-group");
        inactiveGroup.setIsActive(Boolean.FALSE);
        groups.add(inactiveGroup);
        product.setModifierGroups(groups);
        product.setDisplayOrder(2);
        return product;
    }
}
