package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.product.PauseProductRequest;
import com.tap2eat.catalog.dtos.request.product.ProductReorderItemRequest;
import com.tap2eat.catalog.dtos.request.product.ReorderProductsRequest;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.enums.TemporaryUnavailabilityReason;
import com.tap2eat.catalog.services.IProductService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    private final IProductService service = mock(IProductService.class);
    private final ProductController controller = new ProductController(service, new CatalogResponseMapper());

    @Test
    void productEndpoints_shouldDelegateToServiceAndMapResponse() {
        ProductDocument product = CatalogTestDataFactory.simpleProduct();
        PauseProductRequest pauseRequest = new PauseProductRequest(TemporaryUnavailabilityReason.OUT_OF_STOCK, null);
        ReorderProductsRequest reorderRequest = new ReorderProductsRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                List.of(new ProductReorderItemRequest(CatalogTestDataFactory.PRODUCT_ID, 1))
        );
        when(service.createProduct(CatalogTestDataFactory.createSimpleProductRequest())).thenReturn(product);
        when(service.updateProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.updateProductRequest())).thenReturn(product);
        when(service.getProductById(CatalogTestDataFactory.PRODUCT_ID)).thenReturn(product);
        when(service.getProductsByRestaurant(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(List.of(product));
        when(service.getProductsByCategory(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(List.of(product));
        when(service.deactivateProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID)).thenReturn(product);
        when(service.pauseProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID, pauseRequest)).thenReturn(product);
        when(service.resumeProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID)).thenReturn(product);
        when(service.activateProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID)).thenReturn(product);
        when(service.deleteProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID)).thenReturn(product);
        when(service.restoreProduct(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.PRODUCT_ID)).thenReturn(product);
        when(service.reorderProducts(reorderRequest)).thenReturn(List.of(product));

        assertThat(controller.createProduct(CatalogTestDataFactory.createSimpleProductRequest()).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.updateProduct(CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.updateProductRequest()).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.getProductById(CatalogTestDataFactory.PRODUCT_ID).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.getProductsByRestaurant(CatalogTestDataFactory.RESTAURANT_ID)).hasSize(1);
        assertThat(controller.getProductsByCategory(CatalogTestDataFactory.CATEGORY_ID)).hasSize(1);
        assertThat(controller.deactivateProduct(CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.pauseProduct(CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.RESTAURANT_ID, pauseRequest).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.resumeProduct(CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.activateProduct(CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.deleteProduct(CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.restoreProduct(CatalogTestDataFactory.PRODUCT_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.PRODUCT_ID);
        assertThat(controller.reorderProducts(reorderRequest)).hasSize(1);
    }
}
