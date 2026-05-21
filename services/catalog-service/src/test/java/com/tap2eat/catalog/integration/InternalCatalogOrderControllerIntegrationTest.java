package com.tap2eat.catalog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tap2eat.catalog.config.MongoIntegrationTestBase;
import com.tap2eat.catalog.dtos.request.internal.ValidateOrderItemRequest;
import com.tap2eat.catalog.dtos.request.internal.ValidateOrderRequest;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.BranchDocument;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "tap2eat.internal.service-token=test-internal-token")
@AutoConfigureMockMvc
class InternalCatalogOrderControllerIntegrationTest extends MongoIntegrationTestBase {

    private static final String VALIDATE_PATH = "/internal/catalog/orders/validate";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IRestaurantRepository restaurantRepository;

    @Autowired
    private IBranchRepository branchRepository;

    @Autowired
    private ICategoryRepository categoryRepository;

    @Autowired
    private IProductRepository productRepository;

    @Test
    void validateOrder_shouldReturnValidatedSnapshot() throws Exception {
        seedCatalog(CatalogTestDataFactory.customizableProduct());

        mockMvc.perform(post(VALIDATE_PATH)
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest(List.of("option-1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.restaurantId").value(CatalogTestDataFactory.RESTAURANT_ID))
                .andExpect(jsonPath("$.items[0].productName").value("Al pastor"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(35))
                .andExpect(jsonPath("$.items[0].selectedModifiers[0].modifierOptionName").value("Verde"))
                .andExpect(jsonPath("$.items[0].subtotal").value(80))
                .andExpect(jsonPath("$.total").value(80));
    }

    @Test
    void validateOrder_whenProductDoesNotExist_shouldReturnBadRequest() throws Exception {
        seedCatalog(CatalogTestDataFactory.simpleProduct());

        ValidateOrderRequest request = new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest("missing-product", 2, List.of()))
        );

        postInvalid(request);
    }

    @Test
    void validateOrder_whenProductIsDeleted_shouldReturnBadRequest() throws Exception {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setIsActive(Boolean.FALSE);
        product.setDeletedAt(LocalDateTime.now());
        seedCatalog(product);

        postInvalid(validRequest(List.of()));
    }

    @Test
    void validateOrder_whenRestaurantIsDeleted_shouldReturnBadRequest() throws Exception {
        seedCatalog(CatalogTestDataFactory.simpleProduct());
        RestaurantDocument restaurant = restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID).orElseThrow();
        restaurant.setIsActive(Boolean.FALSE);
        restaurant.setDeletedAt(LocalDateTime.now());
        restaurantRepository.save(restaurant);

        postInvalid(validRequest(List.of()));
    }

    @Test
    void validateOrder_whenBranchIsDeleted_shouldReturnBadRequest() throws Exception {
        seedCatalog(CatalogTestDataFactory.simpleProduct());
        BranchDocument branch = branchRepository.findById(CatalogTestDataFactory.BRANCH_ID).orElseThrow();
        branch.setIsActive(Boolean.FALSE);
        branch.setDeletedAt(LocalDateTime.now());
        branchRepository.save(branch);

        postInvalid(validRequest(List.of()));
    }

    @Test
    void validateOrder_whenProductIsUnavailable_shouldReturnBadRequest() throws Exception {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        AvailabilityConfig availability = CatalogTestDataFactory.openAvailability();
        availability.setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        product.setAvailability(availability);
        seedCatalog(product);

        postInvalid(validRequest(List.of()));
    }

    @Test
    void validateOrder_whenModifierOptionIsInvalid_shouldReturnBadRequest() throws Exception {
        seedCatalog(CatalogTestDataFactory.customizableProduct());

        postInvalid(validRequest(List.of("missing-option")));
    }

    @Test
    void validateOrder_whenQuantityIsInvalid_shouldReturnBadRequest() throws Exception {
        seedCatalog(CatalogTestDataFactory.simpleProduct());

        ValidateOrderRequest request = new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(CatalogTestDataFactory.PRODUCT_ID, 0, List.of()))
        );

        postInvalid(request);
    }

    @Test
    void validateOrder_whenSchedulesAreEmpty_shouldTreatResourcesAsAvailable() throws Exception {
        AvailabilityConfig emptySchedule = new AvailabilityConfig();
        emptySchedule.setStatus(AvailabilityStatus.AVAILABLE);
        emptySchedule.setWeeklySchedule(List.of());
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant();
        BranchDocument branch = CatalogTestDataFactory.branch(
                CatalogTestDataFactory.BRANCH_ID,
                CatalogTestDataFactory.RESTAURANT_ID,
                emptySchedule);
        CategoryDocument category = CatalogTestDataFactory.category(
                CatalogTestDataFactory.CATEGORY_ID,
                CatalogTestDataFactory.RESTAURANT_ID,
                emptySchedule);
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setAvailability(emptySchedule);

        restaurantRepository.save(restaurant);
        branchRepository.save(branch);
        categoryRepository.save(category);
        productRepository.save(product);

        mockMvc.perform(post(VALIDATE_PATH)
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest(List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(70));
    }

    private void seedCatalog(ProductDocument product) {
        restaurantRepository.save(CatalogTestDataFactory.restaurant());
        branchRepository.save(CatalogTestDataFactory.branch());
        categoryRepository.save(CatalogTestDataFactory.category());
        productRepository.save(product);
    }

    private ValidateOrderRequest validRequest(List<String> optionIds) {
        return new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(CatalogTestDataFactory.PRODUCT_ID, 2, optionIds))
        );
    }

    private void postInvalid(ValidateOrderRequest request) throws Exception {
        mockMvc.perform(post(VALIDATE_PATH)
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").exists());
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
