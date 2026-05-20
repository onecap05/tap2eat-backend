package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.services.ICategoryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoryControllerTest {

    private final ICategoryService service = mock(ICategoryService.class);
    private final CategoryController controller = new CategoryController(service, new CatalogResponseMapper());

    @Test
    void categoryEndpoints_shouldDelegateToServiceAndMapResponse() {
        CategoryDocument category = CatalogTestDataFactory.category();
        when(service.createCategory(CatalogTestDataFactory.createCategoryRequest())).thenReturn(category);
        when(service.updateCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.updateCategoryRequest())).thenReturn(category);
        when(service.getCategoryById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(category);
        when(service.getCategoriesByRestaurant(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(List.of(category));
        when(service.deactivateCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID)).thenReturn(category);
        when(service.activateCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID)).thenReturn(category);
        when(service.deleteCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID)).thenReturn(category);

        assertThat(controller.createCategory(CatalogTestDataFactory.createCategoryRequest()).id()).isEqualTo(CatalogTestDataFactory.CATEGORY_ID);
        assertThat(controller.updateCategory(CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.updateCategoryRequest()).id()).isEqualTo(CatalogTestDataFactory.CATEGORY_ID);
        assertThat(controller.getCategoryById(CatalogTestDataFactory.CATEGORY_ID).id()).isEqualTo(CatalogTestDataFactory.CATEGORY_ID);
        assertThat(controller.getCategoriesByRestaurant(CatalogTestDataFactory.RESTAURANT_ID)).hasSize(1);
        assertThat(controller.deactivateCategory(CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.CATEGORY_ID);
        assertThat(controller.activateCategory(CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.CATEGORY_ID);
        assertThat(controller.deleteCategory(CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.CATEGORY_ID);
    }
}
