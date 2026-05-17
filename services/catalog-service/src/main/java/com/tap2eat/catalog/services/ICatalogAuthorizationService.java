package com.tap2eat.catalog.services;

public interface ICatalogAuthorizationService {

    String getCurrentAccountId();

    void validateCurrentAccountMatchesOwner(String ownerAccountId);

    void validateCurrentAccountOwnsRestaurant(String restaurantId);
}