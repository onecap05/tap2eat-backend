package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.response.customer.CustomerRestaurantResponse;
import com.tap2eat.catalog.services.ICustomerCatalogService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerCatalogControllerTest {

    private final ICustomerCatalogService service = mock(ICustomerCatalogService.class);
    private final CustomerCatalogController controller = new CustomerCatalogController(service);

    @Test
    void customerEndpoints_shouldDelegateToService() {
        when(service.getActiveRestaurants()).thenReturn(List.of(new CustomerRestaurantResponse("r1", "Demo", null, null, null, true, true)));
        when(service.searchActiveRestaurants("tacos")).thenReturn(List.of(new CustomerRestaurantResponse("r1", "Demo", null, null, null, true, true)));

        assertThat(controller.getRestaurants()).hasSize(1);
        assertThat(controller.searchRestaurants("tacos")).hasSize(1);

        controller.getRestaurant("r1");
        controller.getRestaurantBranches("r1");
        controller.getRestaurantCategories("r1");
        controller.getRestaurantProducts("r1");
        controller.getProduct("p1");
    }
}
