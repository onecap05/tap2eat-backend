package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.response.branch.BranchResponse;
import com.tap2eat.catalog.dtos.response.category.CategoryResponse;
import com.tap2eat.catalog.dtos.response.product.ProductResponse;
import com.tap2eat.catalog.dtos.response.restaurant.RestaurantResponse;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.services.ICustomerCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerCatalogController {

    private final ICustomerCatalogService customerCatalogService;
    private final CatalogResponseMapper catalogResponseMapper;

    @GetMapping("/restaurants")
    public List<RestaurantResponse> getRestaurants() {
        return catalogResponseMapper.toRestaurantResponses(customerCatalogService.getActiveRestaurants());
    }

    @GetMapping("/restaurants/{restaurantId}")
    public RestaurantResponse getRestaurant(@PathVariable String restaurantId) {
        return catalogResponseMapper.toRestaurantResponse(customerCatalogService.getActiveRestaurantById(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/branches")
    public List<BranchResponse> getRestaurantBranches(@PathVariable String restaurantId) {
        return catalogResponseMapper.toBranchResponses(customerCatalogService.getActiveBranchesByRestaurant(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/categories")
    public List<CategoryResponse> getRestaurantCategories(@PathVariable String restaurantId) {
        return catalogResponseMapper.toCategoryResponses(customerCatalogService.getActiveCategoriesByRestaurant(restaurantId));
    }

    @GetMapping("/restaurants/{restaurantId}/products")
    public List<ProductResponse> getRestaurantProducts(@PathVariable String restaurantId) {
        return catalogResponseMapper.toProductResponses(customerCatalogService.getAvailableProductsByRestaurant(restaurantId));
    }

    @GetMapping("/products/{productId}")
    public ProductResponse getProduct(@PathVariable String productId) {
        return catalogResponseMapper.toProductResponse(customerCatalogService.getAvailableProductById(productId));
    }
}
