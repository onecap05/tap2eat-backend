package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.RestaurantMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.ICatalogAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private ICatalogAuthorizationService authorizationService;

    @Mock
    private IBranchRepository branchRepository;

    private RestaurantServiceImpl restaurantService;

    @BeforeEach
    void setUp() {
        restaurantService = new RestaurantServiceImpl(
                restaurantRepository,
                authorizationService,
                branchRepository,
                new RestaurantMapper()
        );
    }

    @Test
    void createRestaurant_shouldCreateValidRestaurantForOwner() {
        when(restaurantRepository.findByOwnerAccountId(CatalogTestDataFactory.OWNER_ID)).thenReturn(Optional.empty());
        when(restaurantRepository.save(any(RestaurantDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantDocument restaurant = restaurantService.createRestaurant(CatalogTestDataFactory.createRestaurantRequest());

        assertThat(restaurant.getOwnerAccountId()).isEqualTo(CatalogTestDataFactory.OWNER_ID);
        assertThat(restaurant.getName()).isEqualTo("Demo Restaurant");
        assertThat(restaurant.getIsActive()).isTrue();
        verify(authorizationService).validateCurrentAccountMatchesOwner(CatalogTestDataFactory.OWNER_ID);
    }

    @Test
    void createRestaurant_shouldFailWhenOwnerAlreadyHasRestaurantOrRequestInvalid() {
        when(restaurantRepository.findByOwnerAccountId(CatalogTestDataFactory.OWNER_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.restaurant()));

        assertThatThrownBy(() -> restaurantService.createRestaurant(CatalogTestDataFactory.createRestaurantRequest()))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> restaurantService.createRestaurant(null))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getRestaurantByOwner_shouldReturnRestaurantOrFailWhenMissing() {
        when(restaurantRepository.findByOwnerAccountId(CatalogTestDataFactory.OWNER_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.restaurant()));

        RestaurantDocument restaurant = restaurantService.getRestaurantByOwnerAccountId(CatalogTestDataFactory.OWNER_ID);

        assertThat(restaurant.getId()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        verify(authorizationService).validateCurrentAccountMatchesOwner(CatalogTestDataFactory.OWNER_ID);

        when(restaurantRepository.findByOwnerAccountId("owner-missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> restaurantService.getRestaurantByOwnerAccountId("owner-missing"))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getRestaurantById_shouldValidateOwnerAndReturnRestaurant() {
        when(restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.restaurant()));

        RestaurantDocument restaurant = restaurantService.getRestaurantById(CatalogTestDataFactory.RESTAURANT_ID);

        assertThat(restaurant.getId()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        verify(authorizationService).validateCurrentAccountOwnsRestaurant(CatalogTestDataFactory.RESTAURANT_ID);
    }

    @Test
    void updateRestaurant_shouldUpdateNameDescriptionAndLogo() {
        RestaurantDocument existing = CatalogTestDataFactory.restaurant();
        when(restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(Optional.of(existing));
        when(restaurantRepository.save(any(RestaurantDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantDocument updated = restaurantService.updateRestaurant(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.OWNER_ID,
                CatalogTestDataFactory.updateRestaurantRequest()
        );

        assertThat(updated.getName()).isEqualTo("Updated Restaurant");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getLogo().getObjectKey()).isEqualTo("tap2eat/tests/request");
    }

    @Test
    void updateRestaurant_shouldFailForInvalidInputMissingRestaurantOrWrongOwner() {
        assertThatThrownBy(() -> restaurantService.updateRestaurant(" ", CatalogTestDataFactory.OWNER_ID, CatalogTestDataFactory.updateRestaurantRequest()))
                .isInstanceOf(CatalogValidationException.class);

        when(restaurantRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> restaurantService.updateRestaurant("missing", CatalogTestDataFactory.OWNER_ID, CatalogTestDataFactory.updateRestaurantRequest()))
                .isInstanceOf(CatalogValidationException.class);

        RestaurantDocument otherOwner = CatalogTestDataFactory.restaurant(CatalogTestDataFactory.RESTAURANT_ID, "owner-2");
        when(restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(Optional.of(otherOwner));
        assertThatThrownBy(() -> restaurantService.updateRestaurant(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.OWNER_ID,
                CatalogTestDataFactory.updateRestaurantRequest()
        )).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void deactivateRestaurant_shouldSoftDeleteWhenNoActiveBranches() {
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant();
        when(restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrue(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(List.of());
        when(restaurantRepository.save(any(RestaurantDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantDocument deleted = restaurantService.deactivateRestaurant(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.OWNER_ID
        );

        assertThat(deleted.getIsActive()).isFalse();
        assertThat(deleted.getDeletedAt()).isNotNull();
    }

    @Test
    void deactivateRestaurant_shouldBlockWhenRestaurantHasActiveBranches() {
        BranchDocument activeBranch = CatalogTestDataFactory.branch();
        when(restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.restaurant()));
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrue(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(List.of(activeBranch));

        assertThatThrownBy(() -> restaurantService.deactivateRestaurant(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.OWNER_ID
        )).isInstanceOf(CatalogValidationException.class);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void activateRestaurant_shouldRestoreWithoutPhysicalDelete() {
        RestaurantDocument deleted = CatalogTestDataFactory.deletedRestaurant(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.OWNER_ID
        );
        when(restaurantRepository.findById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(Optional.of(deleted));
        when(restaurantRepository.save(any(RestaurantDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantDocument restored = restaurantService.activateRestaurant(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.OWNER_ID
        );

        assertThat(restored.getIsActive()).isTrue();
        assertThat(restored.getDeletedAt()).isNull();
        ArgumentCaptor<RestaurantDocument> captor = ArgumentCaptor.forClass(RestaurantDocument.class);
        verify(restaurantRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
    }

    @Test
    void operations_shouldRejectInvalidOwnerOrBlankIds() {
        assertThatThrownBy(() -> restaurantService.getRestaurantById(" "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> restaurantService.getRestaurantByOwnerAccountId(" "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> restaurantService.deactivateRestaurant(" ", CatalogTestDataFactory.OWNER_ID))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> restaurantService.activateRestaurant(CatalogTestDataFactory.RESTAURANT_ID, " "))
                .isInstanceOf(CatalogValidationException.class);
    }
}
