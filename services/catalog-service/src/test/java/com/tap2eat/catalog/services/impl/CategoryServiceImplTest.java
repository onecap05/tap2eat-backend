package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.CategoryMapper;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.ICatalogAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private ICategoryRepository categoryRepository;

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private ICatalogAuthorizationService authorizationService;

    @Mock
    private IProductRepository productRepository;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(
                categoryRepository,
                restaurantRepository,
                authorizationService,
                new CategoryMapper(),
                productRepository
        );
    }

    @Test
    void createCategory_shouldCreateValidCategory() {
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(categoryRepository.save(any(CategoryDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDocument category = categoryService.createCategory(CatalogTestDataFactory.createCategoryRequest());

        assertThat(category.getRestaurantId()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(category.getName()).isEqualTo("Tacos");
        assertThat(category.getIsActive()).isTrue();
        verify(authorizationService).validateCurrentAccountOwnsRestaurant(CatalogTestDataFactory.RESTAURANT_ID);
    }

    @Test
    void createCategory_shouldRejectInvalidRequestOrMissingRestaurant() {
        assertThatThrownBy(() -> categoryService.createCategory(null)).isInstanceOf(CatalogValidationException.class);
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.createCategory(CatalogTestDataFactory.createCategoryRequest()))
                .isInstanceOf(CatalogValidationException.class);

        CreateCategoryRequest invalid = new CreateCategoryRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                " ",
                "Invalid",
                1,
                null,
                CatalogTestDataFactory.availabilityRequest()
        );
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(invalid)).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getCategoriesByRestaurant_shouldListOnlyActiveRepositoryResults() {
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(categoryRepository.findAllByRestaurantIdAndIsActiveTrue(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(List.of(CatalogTestDataFactory.category()));

        List<CategoryDocument> categories = categoryService.getCategoriesByRestaurant(CatalogTestDataFactory.RESTAURANT_ID);

        assertThat(categories).hasSize(1);
        verify(authorizationService).validateCurrentAccountOwnsRestaurant(CatalogTestDataFactory.RESTAURANT_ID);
    }

    @Test
    void getCategoryById_shouldReturnCategoryAndValidateOwner() {
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.category()));

        CategoryDocument category = categoryService.getCategoryById(CatalogTestDataFactory.CATEGORY_ID);

        assertThat(category.getId()).isEqualTo(CatalogTestDataFactory.CATEGORY_ID);
        verify(authorizationService).validateCurrentAccountOwnsRestaurant(CatalogTestDataFactory.RESTAURANT_ID);
    }

    @Test
    void getCategoryByIdAndList_shouldRejectBlankOrMissingResources() {
        assertThatThrownBy(() -> categoryService.getCategoryById(" "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> categoryService.getCategoriesByRestaurant(" "))
                .isInstanceOf(CatalogValidationException.class);
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.getCategoryById("missing"))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void updateCategory_shouldUpdateNameDescriptionImageAndAvailability() {
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.category()));
        when(categoryRepository.save(any(CategoryDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDocument updated = categoryService.updateCategory(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.CATEGORY_ID,
                CatalogTestDataFactory.updateCategoryRequest()
        );

        assertThat(updated.getName()).isEqualTo("Bebidas");
        assertThat(updated.getDescription()).isEqualTo("Drinks");
        assertThat(updated.getDisplayOrder()).isEqualTo(2);
        assertThat(updated.getImage().getObjectKey()).isEqualTo("tap2eat/tests/request");
    }

    @Test
    void updateCategory_shouldRejectWrongRestaurantOrInvalidInput() {
        assertThatThrownBy(() -> categoryService.updateCategory(" ", CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.updateCategoryRequest()))
                .isInstanceOf(CatalogValidationException.class);
        CategoryDocument category = CatalogTestDataFactory.category(CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.openAvailability());
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.updateCategory("other-restaurant", CatalogTestDataFactory.CATEGORY_ID, CatalogTestDataFactory.updateCategoryRequest()))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void deactivateAndActivateCategory_shouldSoftDeleteAndRestore() {
        CategoryDocument category = CatalogTestDataFactory.category();
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(CategoryDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDocument deleted = categoryService.deactivateCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);

        assertThat(deleted.getIsActive()).isFalse();
        assertThat(deleted.getDeletedAt()).isNotNull();

        CategoryDocument restored = categoryService.activateCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);

        assertThat(restored.getIsActive()).isTrue();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void deactivateActivateAndDelete_shouldRejectBlankInputs() {
        assertThatThrownBy(() -> categoryService.deactivateCategory(" ", CatalogTestDataFactory.CATEGORY_ID))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> categoryService.deactivateCategory(CatalogTestDataFactory.RESTAURANT_ID, " "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> categoryService.activateCategory(" ", CatalogTestDataFactory.CATEGORY_ID))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> categoryService.activateCategory(CatalogTestDataFactory.RESTAURANT_ID, " "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> categoryService.deleteCategory(" ", CatalogTestDataFactory.CATEGORY_ID))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> categoryService.deleteCategory(CatalogTestDataFactory.RESTAURANT_ID, " "))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void deleteCategory_shouldSoftDeleteWhenNoActiveProducts() {
        CategoryDocument category = CatalogTestDataFactory.category();
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdAndIsActiveTrue(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(false);
        when(categoryRepository.save(any(CategoryDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDocument deleted = categoryService.deleteCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID);

        assertThat(deleted.getIsActive()).isFalse();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteCategory_shouldBlockWhenCategoryHasActiveProducts() {
        when(categoryRepository.findById(CatalogTestDataFactory.CATEGORY_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.category()));
        when(productRepository.existsByCategoryIdAndIsActiveTrue(CatalogTestDataFactory.CATEGORY_ID)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.CATEGORY_ID))
                .isInstanceOf(CatalogValidationException.class);
    }
}
