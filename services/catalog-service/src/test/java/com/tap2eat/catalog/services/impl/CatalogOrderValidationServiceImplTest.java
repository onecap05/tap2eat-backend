package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.internal.ValidateOrderItemRequest;
import com.tap2eat.catalog.dtos.request.internal.ValidateOrderRequest;
import com.tap2eat.catalog.dtos.response.internal.ValidateOrderResponse;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
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
import com.tap2eat.catalog.services.IAvailabilityEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogOrderValidationServiceImplTest {

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

    private CatalogOrderValidationServiceImpl service;

    @BeforeEach
    void setUp() {
        IAvailabilityEvaluator availabilityEvaluator = new AvailabilityEvaluatorImpl(FIXED_CLOCK);
        service = new CatalogOrderValidationServiceImpl(
                restaurantRepository,
                branchRepository,
                categoryRepository,
                productRepository,
                availabilityEvaluator
        );
    }

    @Test
    void validateOrder_shouldReturnValidatedSnapshotAndSubtotal() {
        seedValidCatalog(CatalogTestDataFactory.customizableProduct());

        ValidateOrderResponse response = service.validateOrder(request("option-1"));

        assertThat(response.valid()).isTrue();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().productName()).isEqualTo("Al pastor");
        assertThat(response.items().getFirst().selectedModifiers()).hasSize(1);
        assertThat(response.items().getFirst().subtotal()).isEqualByComparingTo(BigDecimal.valueOf(80));
        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(80));
    }

    @Test
    void validateOrder_whenProductDoesNotExist_shouldReject() {
        seedRestaurantAndBranch();
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.PRODUCT_ID))
                .thenReturn(Optional.empty());

        assertInvalid(request());
    }

    @Test
    void validateOrder_whenProductIsDeleted_shouldReject() {
        seedRestaurantAndBranch();
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.PRODUCT_ID))
                .thenReturn(Optional.empty());

        assertInvalid(request());
    }

    @Test
    void validateOrder_whenRestaurantIsDeleted_shouldReject() {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.empty());

        assertInvalid(request());
    }

    @Test
    void validateOrder_whenBranchIsDeleted_shouldReject() {
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant();
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.of(restaurant));
        BranchDocument branch = CatalogTestDataFactory.branch();
        branch.setIsActive(Boolean.FALSE);
        branch.setDeletedAt(LocalDateTime.now());
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID)).thenReturn(Optional.of(branch));

        assertInvalid(request());
    }

    @Test
    void validateOrder_whenProductIsUnavailable_shouldReject() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        AvailabilityConfig availability = CatalogTestDataFactory.openAvailability();
        availability.setStatus(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        product.setAvailability(availability);
        seedRestaurantBranchAndProduct(product);

        assertInvalid(request());
    }

    @Test
    void validateOrder_whenModifierOptionIsInvalid_shouldReject() {
        seedValidCatalog(CatalogTestDataFactory.customizableProduct());

        assertInvalid(request("missing-option"));
    }

    @Test
    void validateOrder_whenQuantityIsInvalid_shouldReject() {
        seedRestaurantAndBranch();

        assertInvalid(new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(CatalogTestDataFactory.PRODUCT_ID, 0, List.of()))
        ));
    }

    @Test
    void validateOrder_shouldRejectIncompleteRequestsAndItems() {
        assertInvalid(null);
        assertInvalid(new ValidateOrderRequest(" ", CatalogTestDataFactory.BRANCH_ID, List.of()));
        assertInvalid(new ValidateOrderRequest(CatalogTestDataFactory.RESTAURANT_ID, " ", List.of()));
        assertInvalid(new ValidateOrderRequest(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.BRANCH_ID, null));
        assertInvalid(new ValidateOrderRequest(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.BRANCH_ID, List.of()));

        seedRestaurantAndBranch();

        assertInvalid(new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                java.util.Collections.singletonList(null)
        ));
        assertInvalid(new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(" ", 1, List.of()))
        ));
        assertInvalid(new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(CatalogTestDataFactory.PRODUCT_ID, null, List.of()))
        ));
        assertInvalid(new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(CatalogTestDataFactory.PRODUCT_ID, -1, List.of()))
        ));
    }

    @Test
    void validateOrder_shouldRejectClosedOrForeignBranch() {
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant();
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.of(restaurant));
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.branch(
                        CatalogTestDataFactory.BRANCH_ID,
                        CatalogTestDataFactory.RESTAURANT_ID,
                        CatalogTestDataFactory.closedAvailability()
                )));

        assertInvalid(request());

        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.branch(
                        CatalogTestDataFactory.BRANCH_ID,
                        "other-restaurant",
                        CatalogTestDataFactory.openAvailability()
                )));

        assertInvalid(request());
    }

    @Test
    void validateOrder_shouldRejectForeignProductOrUnavailableCategory() {
        ProductDocument foreignProduct = CatalogTestDataFactory.simpleProduct(
                CatalogTestDataFactory.PRODUCT_ID,
                "other-restaurant",
                CatalogTestDataFactory.CATEGORY_ID
        );
        seedRestaurantBranchAndProduct(foreignProduct);

        assertInvalid(request());

        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        CategoryDocument unavailableCategory = CatalogTestDataFactory.category(
                CatalogTestDataFactory.CATEGORY_ID,
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.closedAvailability()
        );
        seedBaseCatalog(CatalogTestDataFactory.branch(), unavailableCategory, product);

        assertInvalid(request());
    }

    @Test
    void validateOrder_shouldAllowProductWithoutCategoryAndNullSelectionList() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setCategoryId(null);
        seedRestaurantBranchAndProduct(product);

        ValidateOrderResponse response = service.validateOrder(new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(CatalogTestDataFactory.PRODUCT_ID, 2, null))
        ));

        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

    @Test
    void validateOrder_shouldRejectBlankModifierAndHandleNullModifierCollections() {
        seedValidCatalog(CatalogTestDataFactory.customizableProduct());

        assertInvalid(request(" "));

        ProductDocument productWithoutGroups = CatalogTestDataFactory.simpleProduct();
        productWithoutGroups.setModifierGroups(null);
        seedRestaurantBranchAndProduct(productWithoutGroups);

        assertInvalid(request("missing-option"));

        ProductDocument productWithNullOptions = CatalogTestDataFactory.customizableProduct();
        productWithNullOptions.getModifierGroups().getFirst().setOptions(null);
        seedRestaurantBranchAndProduct(productWithNullOptions);

        assertInvalid(request("missing-option"));
    }

    @Test
    void validateOrder_shouldIgnoreInactiveModifierGroupsAndOptions() {
        ProductDocument product = CatalogTestDataFactory.customizableProduct();
        product.getModifierGroups().getFirst().setIsActive(Boolean.FALSE);
        seedRestaurantBranchAndProduct(product);

        assertInvalid(request("option-1"));

        ProductDocument productWithInactiveOption = CatalogTestDataFactory.customizableProduct();
        productWithInactiveOption.getModifierGroups().getFirst().getOptions().getFirst().setIsActive(Boolean.FALSE);
        seedRestaurantBranchAndProduct(productWithInactiveOption);

        assertInvalid(request("option-1"));
    }

    @Test
    void validateOrder_shouldTreatNullPricesAsZero() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setPrice(null);
        seedValidCatalog(product);

        ValidateOrderResponse response = service.validateOrder(request());

        assertThat(response.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void validateOrder_whenSchedulesAreEmpty_shouldTreatResourcesAsAvailable() {
        AvailabilityConfig emptySchedule = new AvailabilityConfig();
        emptySchedule.setStatus(AvailabilityStatus.AVAILABLE);
        emptySchedule.setWeeklySchedule(List.of());
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
        seedBaseCatalog(branch, category, product);

        ValidateOrderResponse response = service.validateOrder(request());

        assertThat(response.total()).isEqualByComparingTo(BigDecimal.valueOf(70));
    }

    private void seedValidCatalog(ProductDocument product) {
        seedBaseCatalog(CatalogTestDataFactory.branch(), CatalogTestDataFactory.category(), product);
    }

    private void seedBaseCatalog() {
        seedBaseCatalog(
                CatalogTestDataFactory.branch(),
                CatalogTestDataFactory.category(),
                CatalogTestDataFactory.simpleProduct()
        );
    }

    private void seedRestaurantAndBranch() {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.restaurant()));
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.branch()));
    }

    private void seedRestaurantBranchAndProduct(ProductDocument product) {
        seedRestaurantAndBranch();
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.PRODUCT_ID))
                .thenReturn(Optional.of(product));
    }

    private void seedBaseCatalog(BranchDocument branch, CategoryDocument category, ProductDocument product) {
        when(restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.restaurant()));
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID)).thenReturn(Optional.of(branch));
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(category));
        when(productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(CatalogTestDataFactory.PRODUCT_ID))
                .thenReturn(Optional.of(product));
    }

    private ValidateOrderRequest request(String... optionIds) {
        return new ValidateOrderRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                List.of(new ValidateOrderItemRequest(
                        CatalogTestDataFactory.PRODUCT_ID,
                        2,
                        List.of(optionIds)))
        );
    }

    private void assertInvalid(ValidateOrderRequest request) {
        assertThatThrownBy(() -> service.validateOrder(request))
                .isInstanceOf(CatalogValidationException.class);
    }
}
