package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.dtos.request.category.UpdateCategoryRequest;
import com.tap2eat.catalog.dtos.response.category.CategoryResponse;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.services.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService ICategoryService;
    private final CatalogResponseMapper catalogResponseMapper;

    @PostMapping
    public CategoryResponse createCategory(@RequestBody CreateCategoryRequest request) {
        CategoryDocument category = ICategoryService.createCategory(request);
        return catalogResponseMapper.toCategoryResponse(category);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse updateCategory(
            @PathVariable String categoryId,
            @RequestParam String restaurantId,
            @RequestBody UpdateCategoryRequest request
    ) {
        CategoryDocument category = ICategoryService.updateCategory(restaurantId, categoryId, request);
        return catalogResponseMapper.toCategoryResponse(category);
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse getCategoryById(@PathVariable String categoryId) {
        CategoryDocument category = ICategoryService.getCategoryById(categoryId);
        return catalogResponseMapper.toCategoryResponse(category);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<CategoryResponse> getCategoriesByRestaurant(@PathVariable String restaurantId) {
        List<CategoryDocument> categories = ICategoryService.getCategoriesByRestaurant(restaurantId);
        return catalogResponseMapper.toCategoryResponses(categories);
    }

    @PatchMapping("/{categoryId}/deactivate")
    public CategoryResponse deactivateCategory(
            @PathVariable String categoryId,
            @RequestParam String restaurantId
    ) {
        CategoryDocument category = ICategoryService.deactivateCategory(restaurantId, categoryId);
        return catalogResponseMapper.toCategoryResponse(category);
    }

    @PatchMapping("/{categoryId}/activate")
    public CategoryResponse activateCategory(
            @PathVariable String categoryId,
            @RequestParam String restaurantId
    ) {
        CategoryDocument category = ICategoryService.activateCategory(restaurantId, categoryId);
        return catalogResponseMapper.toCategoryResponse(category);
    }
}