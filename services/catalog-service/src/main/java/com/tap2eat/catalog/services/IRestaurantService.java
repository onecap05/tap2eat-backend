package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.models.documents.RestaurantDocument;

public interface IRestaurantService {

    RestaurantDocument createRestaurant(CreateRestaurantRequest request);

    RestaurantDocument updateRestaurant(String restaurantId, String ownerAccountId, UpdateRestaurantRequest request);

    RestaurantDocument getRestaurantById(String restaurantId);

    RestaurantDocument getRestaurantByOwnerAccountId(String ownerAccountId);

    RestaurantDocument deactivateRestaurant(String restaurantId, String ownerAccountId);

    RestaurantDocument activateRestaurant(String restaurantId, String ownerAccountId);
}