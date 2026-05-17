package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.dtos.request.category.UpdateCategoryRequest;
import com.tap2eat.catalog.models.documents.CategoryDocument;

import java.util.List;

public interface ICategoryService {

    CategoryDocument createCategory(CreateCategoryRequest request);

    CategoryDocument updateCategory(String restaurantId, String categoryId, UpdateCategoryRequest request);

    CategoryDocument getCategoryById(String categoryId);

    List<CategoryDocument> getCategoriesByRestaurant(String restaurantId);

    CategoryDocument deactivateCategory(String restaurantId, String categoryId);

    CategoryDocument activateCategory(String restaurantId, String categoryId);

    CategoryDocument deleteCategory(String restaurantId, String categoryId);
}