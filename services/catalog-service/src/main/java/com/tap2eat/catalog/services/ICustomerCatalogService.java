package com.tap2eat.catalog.services;

import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;

import java.util.List;

public interface ICustomerCatalogService {

    List<RestaurantDocument> getActiveRestaurants();

    RestaurantDocument getActiveRestaurantById(String restaurantId);

    List<BranchDocument> getActiveBranchesByRestaurant(String restaurantId);

    List<CategoryDocument> getActiveCategoriesByRestaurant(String restaurantId);

    List<ProductDocument> getAvailableProductsByRestaurant(String restaurantId);

    ProductDocument getAvailableProductById(String productId);
}
