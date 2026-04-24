package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.dtos.request.category.UpdateCategoryRequest;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public CategoryDocument createCategory(@RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @PutMapping("/{categoryId}")
    public CategoryDocument updateCategory(
            @PathVariable String categoryId,
            @RequestParam String restaurantId,
            @RequestBody UpdateCategoryRequest request
    ) {
        return categoryService.updateCategory(restaurantId, categoryId, request);
    }

    @GetMapping("/{categoryId}")
    public CategoryDocument getCategoryById(@PathVariable String categoryId) {
        return categoryService.getCategoryById(categoryId);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<CategoryDocument> getCategoriesByRestaurant(@PathVariable String restaurantId) {
        return categoryService.getCategoriesByRestaurant(restaurantId);
    }

    @PatchMapping("/{categoryId}/deactivate")
    public CategoryDocument deactivateCategory(
            @PathVariable String categoryId,
            @RequestParam String restaurantId
    ) {
        return categoryService.deactivateCategory(restaurantId, categoryId);
    }

    @PatchMapping("/{categoryId}/activate")
    public CategoryDocument activateCategory(
            @PathVariable String categoryId,
            @RequestParam String restaurantId
    ) {
        return categoryService.activateCategory(restaurantId, categoryId);
    }
}