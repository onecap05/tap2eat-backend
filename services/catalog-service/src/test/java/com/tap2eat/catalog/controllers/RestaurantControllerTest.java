package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.services.IRestaurantService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RestaurantControllerTest {

    private final IRestaurantService service = mock(IRestaurantService.class);
    private final RestaurantController controller = new RestaurantController(service, new CatalogResponseMapper());

    @Test
    void ownerRestaurantEndpoints_shouldDelegateToServiceAndMapResponse() {
        RestaurantDocument restaurant = CatalogTestDataFactory.restaurant();
        when(service.createRestaurant(CatalogTestDataFactory.createRestaurantRequest())).thenReturn(restaurant);
        when(service.updateRestaurant(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.OWNER_ID, CatalogTestDataFactory.updateRestaurantRequest())).thenReturn(restaurant);
        when(service.getRestaurantById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(restaurant);
        when(service.getRestaurantByOwnerAccountId(CatalogTestDataFactory.OWNER_ID)).thenReturn(restaurant);
        when(service.deactivateRestaurant(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.OWNER_ID)).thenReturn(restaurant);
        when(service.activateRestaurant(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.OWNER_ID)).thenReturn(restaurant);

        assertThat(controller.createRestaurant(CatalogTestDataFactory.createRestaurantRequest()).id()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(controller.updateRestaurant(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.OWNER_ID, CatalogTestDataFactory.updateRestaurantRequest()).id()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(controller.getRestaurantById(CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(controller.getRestaurantByOwnerAccountId(CatalogTestDataFactory.OWNER_ID).id()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(controller.deactivateRestaurant(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.OWNER_ID).id()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(controller.activateRestaurant(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.OWNER_ID).id()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
    }
}
