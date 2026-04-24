package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.services.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public RestaurantDocument createRestaurant(@RequestBody CreateRestaurantRequest request) {
        return restaurantService.createRestaurant(request);
    }

    @PutMapping("/{restaurantId}")
    public RestaurantDocument updateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId,
            @RequestBody UpdateRestaurantRequest request
    ) {
        return restaurantService.updateRestaurant(restaurantId, ownerAccountId, request);
    }

    @GetMapping("/{restaurantId}")
    public RestaurantDocument getRestaurantById(@PathVariable String restaurantId) {
        return restaurantService.getRestaurantById(restaurantId);
    }

    @GetMapping("/owner/{ownerAccountId}")
    public RestaurantDocument getRestaurantByOwnerAccountId(@PathVariable String ownerAccountId) {
        return restaurantService.getRestaurantByOwnerAccountId(ownerAccountId);
    }

    @PatchMapping("/{restaurantId}/deactivate")
    public RestaurantDocument deactivateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId
    ) {
        return restaurantService.deactivateRestaurant(restaurantId, ownerAccountId);
    }

    @PatchMapping("/{restaurantId}/activate")
    public RestaurantDocument activateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId
    ) {
        return restaurantService.activateRestaurant(restaurantId, ownerAccountId);
    }
}