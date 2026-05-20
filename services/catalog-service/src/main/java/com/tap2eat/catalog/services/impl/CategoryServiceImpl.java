package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.dtos.request.category.UpdateCategoryRequest;
import com.tap2eat.catalog.services.ICatalogAuthorizationService;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.mappers.CategoryMapper;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.services.ICategoryService;
import com.tap2eat.catalog.validators.CategoryValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final ICategoryRepository ICategoryRepository;
    private final IRestaurantRepository IRestaurantRepository;
    private final ICatalogAuthorizationService catalogAuthorizationService;
    private final CategoryMapper categoryMapper;
    private final IProductRepository IProductRepository;


    @Override
    public CategoryDocument createCategory(CreateCategoryRequest request) {
        validateCreateRequest(request);
        validateRestaurantExists(request.restaurantId());
        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(request.restaurantId());

        CategoryDocument category = categoryMapper.toDocument(request);
        CategoryValidator.validate(category);

        return ICategoryRepository.save(category);
    }

    @Override
    public CategoryDocument updateCategory(String restaurantId, String categoryId, UpdateCategoryRequest request) {
        if (isBlank(restaurantId) || isBlank(categoryId) || request == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        CategoryDocument category = getCategoryOrThrow(categoryId);
        validateCategoryOwnership(category, restaurantId);

        categoryMapper.updateDocument(category, request);
        CategoryValidator.validate(category);

        return ICategoryRepository.save(category);
    }

    @Override
    public CategoryDocument getCategoryById(String categoryId) {
        if (isBlank(categoryId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        CategoryDocument category = getCategoryOrThrow(categoryId);
        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(category.getRestaurantId());

        return category;
    }

    @Override
    public List<CategoryDocument> getCategoriesByRestaurant(String restaurantId) {
        if (isBlank(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        validateRestaurantExists(restaurantId);
        return ICategoryRepository.findAllByRestaurantIdAndIsActiveTrue(restaurantId);
    }

    @Override
    public CategoryDocument deactivateCategory(String restaurantId, String categoryId) {
        if (isBlank(restaurantId) || isBlank(categoryId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        CategoryDocument category = getCategoryOrThrow(categoryId);
        validateCategoryOwnership(category, restaurantId);

        category.setIsActive(Boolean.FALSE);
        category.setDeletedAt(LocalDateTime.now());

        return ICategoryRepository.save(category);
    }

    @Override
    public CategoryDocument activateCategory(String restaurantId, String categoryId) {
        if (isBlank(restaurantId) || isBlank(categoryId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        CategoryDocument category = getCategoryOrThrow(categoryId);
        validateCategoryOwnership(category, restaurantId);

        category.setIsActive(Boolean.TRUE);
        category.setDeletedAt(null);

        return ICategoryRepository.save(category);
    }

    @Override
    public CategoryDocument deleteCategory(String restaurantId, String categoryId) {
        validateCategoryOperationRequest(restaurantId, categoryId);

        CategoryDocument category = getCategoryOrThrow(categoryId);
        validateCategoryOwnership(category, restaurantId);
        validateCategoryHasNoActiveProducts(categoryId);

        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        category.setIsActive(Boolean.FALSE);
        category.setDeletedAt(LocalDateTime.now());

        return ICategoryRepository.save(category);
    }

    private void validateCategoryOperationRequest(String restaurantId, String categoryId) {
        if (isBlank(restaurantId) || isBlank(categoryId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }
    }

    private void validateCategoryHasNoActiveProducts(String categoryId) {
        if (IProductRepository.existsByCategoryIdAndIsActiveTrue(categoryId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }
    }

    private void validateCreateRequest(CreateCategoryRequest request) {
        if (request == null || isBlank(request.restaurantId())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_CATEGORY_DATA);
        }
    }

    private void validateRestaurantExists(String restaurantId) {
        if (!IRestaurantRepository.existsById(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private CategoryDocument getCategoryOrThrow(String categoryId) {
        return ICategoryRepository.findById(categoryId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateCategoryOwnership(CategoryDocument category, String restaurantId) {
        if (category == null || !restaurantId.equals(category.getRestaurantId())) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}