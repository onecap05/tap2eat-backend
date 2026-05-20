package com.tap2eat.catalog.integration;

import com.tap2eat.catalog.config.MongoIntegrationTestBase;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.ProductType;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerCatalogControllerIntegrationTest extends MongoIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IRestaurantRepository restaurantRepository;

    @Autowired
    private IBranchRepository branchRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IProductRepository productRepository;

    @Test
    void getRestaurants_shouldNotRequireOwnerAndShouldMarkOpenState() throws Exception {
        seedOpenRestaurant("restaurant-open", CatalogTestDataFactory.openAvailability());
        seedOpenRestaurant("restaurant-no-schedule", null);
        seedOpenRestaurant("restaurant-empty-schedule", emptySchedule());
        seedClosedRestaurant("restaurant-closed");
        seedDeletedRestaurant("restaurant-deleted");

        mockMvc.perform(get("/api/customer/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value("restaurant-open"))
                .andExpect(jsonPath("$[0].open").value(true))
                .andExpect(jsonPath("$[1].id").value("restaurant-no-schedule"))
                .andExpect(jsonPath("$[1].open").value(true))
                .andExpect(jsonPath("$[2].id").value("restaurant-empty-schedule"))
                .andExpect(jsonPath("$[2].open").value(true))
                .andExpect(jsonPath("$[3].id").value("restaurant-closed"))
                .andExpect(jsonPath("$[3].open").value(false));
    }

    @Test
    void getRestaurantDetail_shouldReturnActiveRestaurant() throws Exception {
        seedOpenRestaurant("restaurant-open", CatalogTestDataFactory.openAvailability());

        mockMvc.perform(get("/api/customer/restaurants/restaurant-open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("restaurant-open"))
                .andExpect(jsonPath("$.open").value(true));
    }

    @Test
    void getRestaurantDetail_shouldReturnNotFoundForDeletedRestaurant() throws Exception {
        seedDeletedRestaurant("restaurant-deleted");

        mockMvc.perform(get("/api/customer/restaurants/restaurant-deleted"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATALOG_008"));
    }

    @Test
    void getBranches_shouldReturnVisibleBranchesWithOpenFlag() throws Exception {
        seedOpenRestaurant("restaurant-open", CatalogTestDataFactory.openAvailability());
        branchRepository.save(branch("branch-closed", "restaurant-open", CatalogTestDataFactory.closedAvailability()));
        BranchDocument deleted = branch("branch-deleted", "restaurant-open", CatalogTestDataFactory.openAvailability());
        deleted.setIsActive(Boolean.FALSE);
        deleted.setDeletedAt(LocalDateTime.now());
        branchRepository.save(deleted);

        mockMvc.perform(get("/api/customer/restaurants/restaurant-open/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].open").value(true))
                .andExpect(jsonPath("$[1].open").value(false));
    }

    @Test
    void getCategories_shouldFilterDeletedAndOutOfScheduleCategories() throws Exception {
        seedOpenRestaurant("restaurant-open", CatalogTestDataFactory.openAvailability());
        categoryRepository.save(category("category-available", "restaurant-open", CatalogTestDataFactory.openAvailability()));
        categoryRepository.save(category("category-out-of-schedule", "restaurant-open", CatalogTestDataFactory.closedAvailability()));
        CategoryDocument deleted = category("category-deleted", "restaurant-open", CatalogTestDataFactory.openAvailability());
        deleted.setIsActive(Boolean.FALSE);
        deleted.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(deleted);

        mockMvc.perform(get("/api/customer/restaurants/restaurant-open/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("category-available"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void getProducts_shouldReturnOnlyCurrentlyAvailableProductsAndModifierGroups() throws Exception {
        seedOpenRestaurant("restaurant-open", CatalogTestDataFactory.openAvailability());
        categoryRepository.save(category("category-available", "restaurant-open", CatalogTestDataFactory.openAvailability()));
        productRepository.save(customizableProduct("product-customizable", "restaurant-open", "category-available"));
        productRepository.save(product("product-paused", "restaurant-open", "category-available", pausedAvailability()));
        productRepository.save(product("product-out-of-schedule", "restaurant-open", "category-available", CatalogTestDataFactory.closedAvailability()));
        ProductDocument deleted = product("product-deleted", "restaurant-open", "category-available", CatalogTestDataFactory.openAvailability());
        deleted.setIsActive(Boolean.FALSE);
        deleted.setDeletedAt(LocalDateTime.now());
        productRepository.save(deleted);

        mockMvc.perform(get("/api/customer/restaurants/restaurant-open/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("product-customizable"))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].modifierGroups", hasSize(1)));
    }

    @Test
    void ownerEndpoints_shouldRemainProtected() throws Exception {
        mockMvc.perform(get("/api/restaurants/restaurant-open"))
                .andExpect(status().isUnauthorized());
    }

    private void seedOpenRestaurant(String restaurantId, AvailabilityConfig branchAvailability) {
        restaurantRepository.save(restaurant(restaurantId, true, false));
        branchRepository.save(branch("branch-" + restaurantId, restaurantId, branchAvailability));
    }

    private void seedClosedRestaurant(String restaurantId) {
        restaurantRepository.save(restaurant(restaurantId, true, false));
        branchRepository.save(branch("branch-" + restaurantId, restaurantId, CatalogTestDataFactory.closedAvailability()));
    }

    private void seedDeletedRestaurant(String restaurantId) {
        restaurantRepository.save(restaurant(restaurantId, false, true));
    }

    private RestaurantDocument restaurant(String id, boolean active, boolean deleted) {
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant(id, "owner-" + id);
        restaurant.setIsActive(active);
        restaurant.setDeletedAt(deleted ? LocalDateTime.now() : null);
        return restaurant;
    }

    private BranchDocument branch(String id, String restaurantId, AvailabilityConfig availability) {
        return CatalogTestDataFactory.branch(id, restaurantId, availability);
    }

    private CategoryDocument category(String id, String restaurantId, AvailabilityConfig availability) {
        return CatalogTestDataFactory.category(id, restaurantId, availability);
    }

    private ProductDocument product(String id, String restaurantId, String categoryId, AvailabilityConfig availability) {
        ProductDocument product = CatalogTestDataFactory.simpleProduct(id, restaurantId, categoryId);
        product.setAvailability(availability);
        return product;
    }

    private ProductDocument customizableProduct(String id, String restaurantId, String categoryId) {
        ProductDocument product = product(id, restaurantId, categoryId, CatalogTestDataFactory.openAvailability());
        product.setProductType(ProductType.CUSTOMIZABLE);
        product.setModifierGroups(List.of(CatalogTestDataFactory.modifierGroup()));
        return product;
    }

    private AvailabilityConfig pausedAvailability() {
        AvailabilityConfig availability = CatalogTestDataFactory.openAvailability();
        availability.setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        return availability;
    }

    private AvailabilityConfig emptySchedule() {
        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(AvailabilityStatus.AVAILABLE);
        availability.setWeeklySchedule(List.of());
        return availability;
    }

    @TestConfiguration
    static class FixedClockTestConfig {

        @Bean
        @Primary
        Clock fixedCatalogClock() {
            return Clock.fixed(
                    Instant.parse("2026-05-20T00:00:00Z"),
                    ZoneId.of("America/Mexico_City")
            );
        }
    }
}
