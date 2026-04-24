package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.dtos.response.restaurant.RestaurantResponse;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.services.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final CatalogResponseMapper catalogResponseMapper;

    @PostMapping
    public RestaurantResponse createRestaurant(@RequestBody CreateRestaurantRequest request) {
        RestaurantDocument restaurant = restaurantService.createRestaurant(request);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @PutMapping("/{restaurantId}")
    public RestaurantResponse updateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId,
            @RequestBody UpdateRestaurantRequest request
    ) {
        RestaurantDocument restaurant = restaurantService.updateRestaurant(restaurantId, ownerAccountId, request);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @GetMapping("/{restaurantId}")
    public RestaurantResponse getRestaurantById(@PathVariable String restaurantId) {
        RestaurantDocument restaurant = restaurantService.getRestaurantById(restaurantId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @GetMapping("/owner/{ownerAccountId}")
    public RestaurantResponse getRestaurantByOwnerAccountId(@PathVariable String ownerAccountId) {
        RestaurantDocument restaurant = restaurantService.getRestaurantByOwnerAccountId(ownerAccountId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @PatchMapping("/{restaurantId}/deactivate")
    public RestaurantResponse deactivateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId
    ) {
        RestaurantDocument restaurant = restaurantService.deactivateRestaurant(restaurantId, ownerAccountId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @PatchMapping("/{restaurantId}/activate")
    public RestaurantResponse activateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId
    ) {
        RestaurantDocument restaurant = restaurantService.activateRestaurant(restaurantId, ownerAccountId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }
}