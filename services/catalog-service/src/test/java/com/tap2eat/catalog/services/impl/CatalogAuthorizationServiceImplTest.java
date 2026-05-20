package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.security.AuthenticatedAccountProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAuthorizationServiceImplTest {

    @Mock
    private AuthenticatedAccountProvider accountProvider;

    @Mock
    private IRestaurantRepository restaurantRepository;

    private CatalogAuthorizationServiceImpl authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new CatalogAuthorizationServiceImpl(accountProvider, restaurantRepository);
    }

    @Test
    void getCurrentAccountId_shouldReturnAuthenticatedAccount() {
        when(accountProvider.getRequiredAccountId()).thenReturn(CatalogTestDataFactory.OWNER_ID);

        assertThat(authorizationService.getCurrentAccountId()).isEqualTo(CatalogTestDataFactory.OWNER_ID);
    }

    @Test
    void validateCurrentAccountMatchesOwner_shouldAllowMatchingOwnerAndRejectMismatch() {
        when(accountProvider.getRequiredAccountId()).thenReturn(CatalogTestDataFactory.OWNER_ID);

        authorizationService.validateCurrentAccountMatchesOwner(CatalogTestDataFactory.OWNER_ID);

        assertThatThrownBy(() -> authorizationService.validateCurrentAccountMatchesOwner("owner-2"))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> authorizationService.validateCurrentAccountMatchesOwner(" "))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void validateCurrentAccountOwnsRestaurant_shouldAllowOwnerAndRejectMissingOrWrongOwner() {
        when(accountProvider.getRequiredAccountId()).thenReturn(CatalogTestDataFactory.OWNER_ID);
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant();
        when(restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(Optional.of(restaurant));

        authorizationService.validateCurrentAccountOwnsRestaurant(CatalogTestDataFactory.RESTAURANT_ID);

        when(restaurantRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authorizationService.validateCurrentAccountOwnsRestaurant("missing"))
                .isInstanceOf(CatalogValidationException.class);

        RestaurantDocument otherOwner = CatalogTestDataFactory.restaurant("restaurant-2", "owner-2");
        when(restaurantRepository.findById("restaurant-2")).thenReturn(Optional.of(otherOwner));
        assertThatThrownBy(() -> authorizationService.validateCurrentAccountOwnsRestaurant("restaurant-2"))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> authorizationService.validateCurrentAccountOwnsRestaurant(" "))
                .isInstanceOf(CatalogValidationException.class);
    }
}
