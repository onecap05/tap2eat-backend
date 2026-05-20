package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.response.customer.CustomerBranchResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerCategoryResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerProductResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerRestaurantResponse;
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

    @GetMapping("/restaurants")
    public List<CustomerRestaurantResponse> getRestaurants() {
        return customerCatalogService.getActiveRestaurants();
    }

    @GetMapping("/restaurants/{restaurantId}")
    public CustomerRestaurantResponse getRestaurant(@PathVariable String restaurantId) {
        return customerCatalogService.getActiveRestaurantById(restaurantId);
    }

    @GetMapping("/restaurants/{restaurantId}/branches")
    public List<CustomerBranchResponse> getRestaurantBranches(@PathVariable String restaurantId) {
        return customerCatalogService.getActiveBranchesByRestaurant(restaurantId);
    }

    @GetMapping("/restaurants/{restaurantId}/categories")
    public List<CustomerCategoryResponse> getRestaurantCategories(@PathVariable String restaurantId) {
        return customerCatalogService.getAvailableCategoriesByRestaurant(restaurantId);
    }

    @GetMapping("/restaurants/{restaurantId}/products")
    public List<CustomerProductResponse> getRestaurantProducts(@PathVariable String restaurantId) {
        return customerCatalogService.getAvailableProductsByRestaurant(restaurantId);
    }

    @GetMapping("/products/{productId}")
    public CustomerProductResponse getProduct(@PathVariable String productId) {
        return customerCatalogService.getAvailableProductById(productId);
    }
}
