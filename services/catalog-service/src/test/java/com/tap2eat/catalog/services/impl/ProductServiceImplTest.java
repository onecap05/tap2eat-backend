package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.config.CatalogImageProperties;
import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.ImageMetadataRequest;
import com.tap2eat.catalog.dtos.request.product.PauseProductRequest;
import com.tap2eat.catalog.dtos.request.product.ProductReorderItemRequest;
import com.tap2eat.catalog.dtos.request.product.ReorderProductsRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.ProductMapper;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import com.tap2eat.catalog.models.enums.ProductType;
import com.tap2eat.catalog.models.enums.TemporaryUnavailabilityReason;
import com.tap2eat.catalog.models.enums.StorageProvider;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.ICatalogAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private IProductRepository productRepository;

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private ICatalogAuthorizationService authorizationService;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        CatalogImageProperties imageProperties = new CatalogImageProperties();
        imageProperties.setDefaultProductUrl("https://cdn.tap2eat.test/default.webp");
        imageProperties.setDefaultProductObjectKey("tap2eat/defaults/default");
        productService = new ProductServiceImpl(
                productRepository,
                restaurantRepository,
                categoryRepository,
                new ProductMapper(),
                imageProperties,
                authorizationService
        );
    }

    @Test
    void createProduct_shouldCreateValidSimpleAndCustomizableProducts() {
        stubRestaurantAndCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        when(productRepository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDocument simple = productService.createProduct(CatalogTestDataFactory.createSimpleProductRequest());
        ProductDocument customizable = productService.createProduct(CatalogTestDataFactory.createCustomizableProductRequest());

        assertThat(simple.getProductType()).isEqualTo(ProductType.SIMPLE);
        assertThat(simple.getModifierGroups()).isEmpty();
        assertThat(customizable.getProductType()).isEqualTo(ProductType.CUSTOMIZABLE);
        assertThat(customizable.getModifierGroups()).hasSize(1);
    }

    @Test
    void createProduct_withoutImage_shouldAssignDefaultImage() {
        stubRestaurantAndCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        when(productRepository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateProductRequest request = new CreateProductRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                "Default image product",
                "No image",
                ProductType.SIMPLE,
                BigDecimal.TEN,
                null,
                List.of(),
                CatalogTestDataFactory.availabilityRequest(),
                Boolean.TRUE,
                1,
                Boolean.FALSE,
                List.of(),
                List.of(),
                List.of()
        );

        ProductDocument product = productService.createProduct(request);

        assertThat(product.getImage().getUrl()).isEqualTo("https://cdn.tap2eat.test/default.webp");
        assertThat(product.getImage().getObjectKey()).isEqualTo("tap2eat/defaults/default");
    }

    @Test
    void createProduct_withBlankOrNullImageUrl_shouldAssignDefaultImage() {
        stubRestaurantAndCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        when(productRepository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDocument blankUrl = productService.createProduct(createProductWithImage(new ImageMetadataRequest(
                " ",
                "object",
                StorageProvider.CLOUDINARY
        )));
        ProductDocument nullUrl = productService.createProduct(createProductWithImage(new ImageMetadataRequest(
                null,
                "object",
                StorageProvider.CLOUDINARY
        )));

        assertThat(blankUrl.getImage().getUrl()).isEqualTo("https://cdn.tap2eat.test/default.webp");
        assertThat(nullUrl.getImage().getUrl()).isEqualTo("https://cdn.tap2eat.test/default.webp");
    }

    @Test
    void createProduct_shouldFailForInvalidCategoryRestaurantOrPrice() {
        assertThatThrownBy(() -> productService.createProduct(null)).isInstanceOf(CatalogValidationException.class);

        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(false);
        assertThatThrownBy(() -> productService.createProduct(CatalogTestDataFactory.createSimpleProductRequest()))
                .isInstanceOf(CatalogValidationException.class);

        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.createProduct(CatalogTestDataFactory.createSimpleProductRequest()))
                .isInstanceOf(CatalogValidationException.class);

        CategoryDocument otherRestaurantCategory = CatalogTestDataFactory.category(CatalogTestDataFactory.CATEGORY_ID, "other-restaurant", CatalogTestDataFactory.openAvailability());
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(otherRestaurantCategory));
        assertThatThrownBy(() -> productService.createProduct(CatalogTestDataFactory.createSimpleProductRequest()))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void createProduct_shouldRejectNegativePrice() {
        stubRestaurantAndCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        CreateProductRequest request = new CreateProductRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                "Bad",
                "Negative",
                ProductType.SIMPLE,
                BigDecimal.valueOf(-1),
                CatalogTestDataFactory.imageRequest(),
                List.of(),
                CatalogTestDataFactory.availabilityRequest(),
                Boolean.TRUE,
                1,
                Boolean.FALSE,
                List.of(),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> productService.createProduct(request)).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void updateProduct_shouldEditFieldsAvailabilityAndModifierGroups() {
        ProductDocument existing = CatalogTestDataFactory.simpleProduct();
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(CatalogTestDataFactory.category()));
        when(productRepository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDocument updated = productService.updateProduct(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.PRODUCT_ID,
                CatalogTestDataFactory.updateProductRequest()
        );

        assertThat(updated.getName()).isEqualTo("Updated taco");
        assertThat(updated.getDescription()).isEqualTo("Updated product");
        assertThat(updated.getPrice()).isEqualByComparingTo("40");
        assertThat(updated.getProductType()).isEqualTo(ProductType.CUSTOMIZABLE);
        assertThat(updated.getModifierGroups()).hasSize(1);
        assertThat(updated.getFeatured()).isFalse();
    }

    @Test
    void updateProduct_shouldSkipCategoryLookupWhenCategoryIdIsBlankThenFailValidation() {
        ProductDocument existing = CatalogTestDataFactory.simpleProduct();
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.updateProduct(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.PRODUCT_ID,
                new UpdateProductRequest(
                        " ",
                        "Name",
                        "Description",
                        ProductType.SIMPLE,
                        BigDecimal.ONE,
                        null,
                        List.of(),
                        CatalogTestDataFactory.availabilityRequest(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        )).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void updateProduct_shouldRejectDeletedOrWrongOwnerProduct() {
        assertThatThrownBy(() -> productService.updateProduct(" ", CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.updateProductRequest()))
                .isInstanceOf(CatalogValidationException.class);
        ProductDocument otherRestaurant = CatalogTestDataFactory.simpleProduct(CatalogTestDataFactory.PRODUCT_ID, "other-restaurant", CatalogTestDataFactory.CATEGORY_ID);
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(otherRestaurant));

        assertThatThrownBy(() -> productService.updateProduct(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.PRODUCT_ID,
                CatalogTestDataFactory.updateProductRequest()
        )).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getProducts_shouldReturnByRestaurantAndCategory() {
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(productRepository.findAllByRestaurantIdAndIsActiveTrue(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(List.of(CatalogTestDataFactory.simpleProduct()));
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.category()));
        when(productRepository.findAllByCategoryIdAndIsActiveTrue(CatalogTestDataFactory.CATEGORY_ID))
                .thenReturn(List.of(CatalogTestDataFactory.simpleProduct()));

        assertThat(productService.getProductsByRestaurant(CatalogTestDataFactory.RESTAURANT_ID)).hasSize(1);
        assertThat(productService.getProductsByCategory(CatalogTestDataFactory.CATEGORY_ID)).hasSize(1);
    }

    @Test
    void getProductByIdAndListMethods_shouldRejectInvalidOrMissingResources() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(product));
        assertThat(productService.getProductById(CatalogTestDataFactory.PRODUCT_ID)).isSameAs(product);

        assertThatThrownBy(() -> productService.getProductById(" ")).isInstanceOf(CatalogValidationException.class);
        when(productRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getProductById("missing")).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.getProductsByRestaurant(" ")).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.getProductsByCategory(" ")).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void pauseAndResumeProduct_shouldChangeAvailability() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDocument paused = productService.pauseProduct(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.PRODUCT_ID,
                new PauseProductRequest(TemporaryUnavailabilityReason.OUT_OF_STOCK, " sold out ")
        );

        assertThat(paused.getAvailability().getStatus()).isEqualTo(AvailabilityStatus.TEMPORARILY_UNAVAILABLE);
        assertThat(paused.getAvailability().getTemporaryReasonDetail()).isEqualTo("sold out");

        ProductDocument resumed = productService.resumeProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID);

        assertThat(resumed.getAvailability().getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(resumed.getAvailability().getTemporaryReason()).isNull();
    }

    @Test
    void pauseAndResumeProduct_shouldCreateAvailabilityWhenMissingAndClearBlankDetail() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        product.setAvailability(null);
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDocument paused = productService.pauseProduct(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.PRODUCT_ID,
                new PauseProductRequest(TemporaryUnavailabilityReason.NO_SUPPLIES, " ")
        );

        assertThat(paused.getAvailability().getTemporaryReasonDetail()).isNull();

        paused.setAvailability(null);
        ProductDocument resumed = productService.resumeProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID);

        assertThat(resumed.getAvailability().getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
    }

    @Test
    void pauseProduct_shouldRejectInvalidRequest() {
        assertThatThrownBy(() -> productService.pauseProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID, null))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.pauseProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID, new PauseProductRequest(null, null)))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void deactivateActivateDeleteAndRestoreProduct_shouldSoftDeleteAndRestore() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.save(any(ProductDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDocument deleted = productService.deleteProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID);

        assertThat(deleted.getIsActive()).isFalse();
        assertThat(deleted.getDeletedAt()).isNotNull();

        ProductDocument restored = productService.restoreProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID);

        assertThat(restored.getIsActive()).isTrue();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void deactivateActivateAndResume_shouldRejectInvalidInputOrWrongRestaurant() {
        assertThatThrownBy(() -> productService.deactivateProduct(" ", CatalogTestDataFactory.PRODUCT_ID))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.activateProduct(CatalogTestDataFactory.RESTAURANT_ID, " "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.resumeProduct(" ", CatalogTestDataFactory.PRODUCT_ID))
                .isInstanceOf(CatalogValidationException.class);
        ProductDocument otherRestaurant = CatalogTestDataFactory.simpleProduct(CatalogTestDataFactory.PRODUCT_ID, "other-restaurant", CatalogTestDataFactory.CATEGORY_ID);
        when(productRepository.findById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(Optional.of(otherRestaurant));

        assertThatThrownBy(() -> productService.deactivateProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void reorderProducts_shouldUpdateOrdersAndValidateOwnership() {
        stubRestaurantAndCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        ProductDocument first = CatalogTestDataFactory.simpleProduct("product-1", CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        ProductDocument second = CatalogTestDataFactory.simpleProduct("product-2", CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        when(productRepository.findAllById(any())).thenReturn(List.of(first, second));
        when(productRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProductDocument> products = productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest("product-1", 2), new ProductReorderItemRequest("product-2", 1))
        ));

        assertThat(products).extracting(ProductDocument::getId).containsExactly("product-2", "product-1");
    }

    @Test
    void reorderProducts_shouldRejectInvalidRequests() {
        assertThatThrownBy(() -> productService.reorderProducts(null)).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(" ", CatalogTestDataFactory.CATEGORY_ID, List.of(new ProductReorderItemRequest("product-1", 1)))))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(CatalogTestDataFactory.RESTAURANT_ID, " ", List.of(new ProductReorderItemRequest("product-1", 1)))))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID, null)))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID, List.of())))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                java.util.Collections.singletonList(null)
        ))).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest(" ", 1))
        ))).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest("product-1", null))
        ))).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest("product-1", -1))
        ))).isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest("product-1", 1), new ProductReorderItemRequest("product-1", 2))
        ))).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void reorderProducts_shouldRejectMissingInactiveOrWrongOwnerProducts() {
        stubRestaurantAndCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        when(productRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest("product-1", 1))
        ))).isInstanceOf(CatalogValidationException.class);

        ProductDocument inactive = CatalogTestDataFactory.simpleProduct("product-1", CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);
        inactive.setIsActive(Boolean.FALSE);
        when(productRepository.findAllById(any())).thenReturn(List.of(inactive));
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest("product-1", 1))
        ))).isInstanceOf(CatalogValidationException.class);

        ProductDocument wrongCategory = CatalogTestDataFactory.simpleProduct("product-1", CatalogTestDataFactory.RESTAURANT_ID, "other-category");
        when(productRepository.findAllById(any())).thenReturn(List.of(wrongCategory));
        assertThatThrownBy(() -> productService.reorderProducts(new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest("product-1", 1))
        ))).isInstanceOf(CatalogValidationException.class);
    }

    private void stubRestaurantAndCategory(String restaurantId, String categoryId) {
        when(restaurantRepository.existsById(restaurantId)).thenReturn(true);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(CatalogTestDataFactory.category(categoryId, restaurantId, CatalogTestDataFactory.openAvailability())));
    }

    private CreateProductRequest createProductWithImage(ImageMetadataRequest image) {
        return new CreateProductRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                "Image fallback",
                "Fallback",
                ProductType.SIMPLE,
                BigDecimal.TEN,
                image,
                List.of(),
                CatalogTestDataFactory.availabilityRequest(),
                Boolean.TRUE,
                1,
                Boolean.FALSE,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
