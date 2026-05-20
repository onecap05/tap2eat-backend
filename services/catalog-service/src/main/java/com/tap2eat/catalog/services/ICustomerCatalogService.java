package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.response.customer.CustomerBranchResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerCategoryResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerProductResponse;
import com.tap2eat.catalog.dtos.response.customer.CustomerRestaurantResponse;

import java.util.List;

public interface ICustomerCatalogService {

    List<CustomerRestaurantResponse> getActiveRestaurants();

    CustomerRestaurantResponse getActiveRestaurantById(String restaurantId);

    List<CustomerBranchResponse> getActiveBranchesByRestaurant(String restaurantId);

    List<CustomerCategoryResponse> getAvailableCategoriesByRestaurant(String restaurantId);

    List<CustomerProductResponse> getAvailableProductsByRestaurant(String restaurantId);

    CustomerProductResponse getAvailableProductById(String productId);
}
