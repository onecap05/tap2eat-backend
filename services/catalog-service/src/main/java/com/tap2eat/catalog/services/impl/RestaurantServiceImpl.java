package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.mappers.RestaurantMapper;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.ICatalogAuthorizationService;
import com.tap2eat.catalog.services.IRestaurantService;
import com.tap2eat.catalog.validators.RestaurantValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements IRestaurantService {

    private final IRestaurantRepository IRestaurantRepository;
    private final ICatalogAuthorizationService catalogAuthorizationService;
    private final RestaurantMapper restaurantMapper;

    @Override
    public RestaurantDocument createRestaurant(CreateRestaurantRequest request) {
        validateCreateRequest(request);
        catalogAuthorizationService.validateCurrentAccountMatchesOwner(request.ownerAccountId());

        IRestaurantRepository.findByOwnerAccountIdAndIsActiveTrue(request.ownerAccountId())
                .ifPresent(existing -> {
                    throw new CatalogValidationException(CatalogErrorCode.RESTAURANT_ALREADY_EXISTS);
                });

        RestaurantDocument restaurant = restaurantMapper.toDocument(request);
        RestaurantValidator.validate(restaurant);

        return IRestaurantRepository.save(restaurant);
    }

    @Override
    public RestaurantDocument updateRestaurant(String restaurantId, String ownerAccountId, UpdateRestaurantRequest request) {
        if (isBlank(restaurantId) || isBlank(ownerAccountId) || request == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountMatchesOwner(ownerAccountId);
        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        RestaurantDocument restaurant = getRestaurantOrThrow(restaurantId);
        validateOwnership(restaurant, ownerAccountId);

        restaurantMapper.updateDocument(restaurant, request);
        RestaurantValidator.validate(restaurant);

        return IRestaurantRepository.save(restaurant);
    }

    @Override
    public RestaurantDocument getRestaurantById(String restaurantId) {
        if (isBlank(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        return getRestaurantOrThrow(restaurantId);
    }

    @Override
    public RestaurantDocument getRestaurantByOwnerAccountId(String ownerAccountId) {
        if (isBlank(ownerAccountId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountMatchesOwner(ownerAccountId);

        return IRestaurantRepository.findByOwnerAccountIdAndIsActiveTrue(ownerAccountId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public RestaurantDocument deactivateRestaurant(String restaurantId, String ownerAccountId) {
        if (isBlank(restaurantId) || isBlank(ownerAccountId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountMatchesOwner(ownerAccountId);
        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        RestaurantDocument restaurant = getRestaurantOrThrow(restaurantId);
        validateOwnership(restaurant, ownerAccountId);

        restaurant.setIsActive(Boolean.FALSE);
        restaurant.setDeletedAt(LocalDateTime.now());

        return IRestaurantRepository.save(restaurant);
    }

    @Override
    public RestaurantDocument activateRestaurant(String restaurantId, String ownerAccountId) {
        if (isBlank(restaurantId) || isBlank(ownerAccountId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }

        catalogAuthorizationService.validateCurrentAccountMatchesOwner(ownerAccountId);
        catalogAuthorizationService.validateCurrentAccountOwnsRestaurant(restaurantId);

        RestaurantDocument restaurant = getRestaurantOrThrow(restaurantId);
        validateOwnership(restaurant, ownerAccountId);

        restaurant.setIsActive(Boolean.TRUE);
        restaurant.setDeletedAt(null);

        return IRestaurantRepository.save(restaurant);
    }

    private void validateCreateRequest(CreateRestaurantRequest request) {
        if (request == null || isBlank(request.ownerAccountId())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_RESTAURANT_DATA);
        }
    }


    private RestaurantDocument getRestaurantOrThrow(String restaurantId) {
        return IRestaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateOwnership(RestaurantDocument restaurant, String ownerAccountId) {
        if (restaurant == null || !ownerAccountId.equals(restaurant.getOwnerAccountId())) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}