package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.ProductType;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "RUN_MONGO_INTEGRATION_TESTS", matches = "true")
class CustomerCatalogControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void getRestaurants_shouldNotRequireOwnerAndShouldMarkOpenState() throws Exception {
        seedOpenRestaurant("restaurant-open");
        seedClosedRestaurant("restaurant-closed");
        seedDeletedRestaurant("restaurant-deleted");

        mockMvc.perform(get("/api/customer/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("restaurant-open"))
                .andExpect(jsonPath("$[0].open").value(true))
                .andExpect(jsonPath("$[1].id").value("restaurant-closed"))
                .andExpect(jsonPath("$[1].open").value(false));
    }

    @Test
    void getProducts_shouldReturnOnlyCurrentlyAvailableProducts() throws Exception {
        seedOpenRestaurant("restaurant-open");
        CategoryDocument category = category("category-available", "restaurant-open", true);
        categoryRepository.save(category);
        productRepository.save(product(
                "product-available",
                "restaurant-open",
                "category-available",
                AvailabilityStatus.AVAILABLE,
                true
        ));
        productRepository.save(product(
                "product-paused",
                "restaurant-open",
                "category-available",
                AvailabilityStatus.TEMPORARILY_UNAVAILABLE,
                true
        ));
        productRepository.save(product(
                "product-out-of-schedule",
                "restaurant-open",
                "category-available",
                AvailabilityStatus.AVAILABLE,
                false
        ));

        mockMvc.perform(get("/api/customer/restaurants/restaurant-open/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("product-available"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void getCategories_shouldReturnOnlyCurrentlyAvailableCategories() throws Exception {
        seedOpenRestaurant("restaurant-open");
        categoryRepository.save(category("category-available", "restaurant-open", true));
        categoryRepository.save(category("category-out-of-schedule", "restaurant-open", false));

        mockMvc.perform(get("/api/customer/restaurants/restaurant-open/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("category-available"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void ownerEndpoints_shouldRemainProtected() throws Exception {
        mockMvc.perform(get("/api/restaurants/restaurant-open"))
                .andExpect(status().isUnauthorized());
    }

    private void seedOpenRestaurant(String restaurantId) {
        restaurantRepository.save(restaurant(restaurantId, true, false));
        branchRepository.save(branch("branch-open", restaurantId, true));
    }

    private void seedClosedRestaurant(String restaurantId) {
        restaurantRepository.save(restaurant(restaurantId, true, false));
        branchRepository.save(branch("branch-closed", restaurantId, false));
    }

    private void seedDeletedRestaurant(String restaurantId) {
        restaurantRepository.save(restaurant(restaurantId, false, true));
    }

    private RestaurantDocument restaurant(String id, boolean active, boolean deleted) {
        RestaurantDocument restaurant = new RestaurantDocument();
        restaurant.setId(id);
        restaurant.setOwnerAccountId("owner-" + id);
        restaurant.setName("Restaurant " + id);
        restaurant.setDescription("Demo");
        restaurant.setIsActive(active);
        restaurant.setDeletedAt(deleted ? LocalDateTime.now() : null);
        return restaurant;
    }

    private BranchDocument branch(String id, String restaurantId, boolean openNow) {
        BranchDocument branch = new BranchDocument();
        branch.setId(id);
        branch.setRestaurantId(restaurantId);
        branch.setName("Branch " + id);
        branch.setFormattedAddress("Address");
        branch.setIsActive(Boolean.TRUE);
        branch.setAvailability(availability(AvailabilityStatus.AVAILABLE, openNow));
        return branch;
    }

    private CategoryDocument category(String id, String restaurantId, boolean availableNow) {
        CategoryDocument category = new CategoryDocument();
        category.setId(id);
        category.setRestaurantId(restaurantId);
        category.setName("Category " + id);
        category.setIsActive(Boolean.TRUE);
        category.setAvailability(availability(AvailabilityStatus.AVAILABLE, availableNow));
        return category;
    }

    private ProductDocument product(
            String id,
            String restaurantId,
            String categoryId,
            AvailabilityStatus status,
            boolean availableNow
    ) {
        ProductDocument product = new ProductDocument();
        product.setId(id);
        product.setRestaurantId(restaurantId);
        product.setCategoryId(categoryId);
        product.setName("Product " + id);
        product.setProductType(ProductType.SIMPLE);
        product.setPrice(BigDecimal.TEN);
        product.setIsActive(Boolean.TRUE);
        product.setAvailability(availability(status, availableNow));
        return product;
    }

    private AvailabilityConfig availability(AvailabilityStatus status, boolean availableNow) {
        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(status);
        availability.setWeeklySchedule(List.of(new DailyAvailability(
                DayOfWeek.TUESDAY,
                Boolean.TRUE,
                List.of(new TimeRange(
                        availableNow ? LocalTime.of(17, 0) : LocalTime.of(8, 0),
                        availableNow ? LocalTime.of(19, 0) : LocalTime.of(10, 0)
                ))
        )));
        return availability;
    }

    private void cleanDatabase() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        branchRepository.deleteAll();
        restaurantRepository.deleteAll();
    }

    @TestConfiguration
    static class FixedClockTestConfig {

        @Bean
        @Primary
        Clock fixedCatalogClock() {
            return Clock.fixed(Instant.parse("2026-05-19T18:00:00Z"), ZoneId.of("UTC"));
        }
    }
}
