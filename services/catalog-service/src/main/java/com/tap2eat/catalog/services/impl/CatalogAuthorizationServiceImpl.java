package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.security.AuthenticatedAccountProvider;
import com.tap2eat.catalog.services.ICatalogAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CatalogAuthorizationServiceImpl implements ICatalogAuthorizationService {

    private final AuthenticatedAccountProvider authenticatedAccountProvider;
    private final IRestaurantRepository restaurantRepository;

    @Override
    public String getCurrentAccountId() {
        return authenticatedAccountProvider.getRequiredAccountId();
    }

    @Override
    public void validateCurrentAccountMatchesOwner(String ownerAccountId) {
        if (isBlank(ownerAccountId)) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }

        String currentAccountId = getCurrentAccountId();

        if (!currentAccountId.equals(ownerAccountId)) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }
    }

    @Override
    public void validateCurrentAccountOwnsRestaurant(String restaurantId) {
        if (isBlank(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        String currentAccountId = getCurrentAccountId();

        RestaurantDocument restaurant = restaurantRepository.findById(restaurantId)
                .filter(existingRestaurant -> Boolean.TRUE.equals(existingRestaurant.getIsActive()))
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));

        if (!currentAccountId.equals(restaurant.getOwnerAccountId())) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}