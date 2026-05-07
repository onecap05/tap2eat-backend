package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.restaurant.CreateRestaurantRequest;
import com.tap2eat.catalog.dtos.request.restaurant.UpdateRestaurantRequest;
import com.tap2eat.catalog.dtos.response.restaurant.RestaurantResponse;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.services.IRestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final IRestaurantService IRestaurantService;
    private final CatalogResponseMapper catalogResponseMapper;

    @PostMapping
    public RestaurantResponse createRestaurant(@RequestBody CreateRestaurantRequest request) {
        RestaurantDocument restaurant = IRestaurantService.createRestaurant(request);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @PutMapping("/{restaurantId}")
    public RestaurantResponse updateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId,
            @RequestBody UpdateRestaurantRequest request
    ) {
        RestaurantDocument restaurant = IRestaurantService.updateRestaurant(restaurantId, ownerAccountId, request);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @GetMapping("/{restaurantId}")
    public RestaurantResponse getRestaurantById(@PathVariable String restaurantId) {
        RestaurantDocument restaurant = IRestaurantService.getRestaurantById(restaurantId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @GetMapping("/owner/{ownerAccountId}")
    public RestaurantResponse getRestaurantByOwnerAccountId(@PathVariable String ownerAccountId) {
        RestaurantDocument restaurant = IRestaurantService.getRestaurantByOwnerAccountId(ownerAccountId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @PatchMapping("/{restaurantId}/deactivate")
    public RestaurantResponse deactivateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId
    ) {
        RestaurantDocument restaurant = IRestaurantService.deactivateRestaurant(restaurantId, ownerAccountId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }

    @PatchMapping("/{restaurantId}/activate")
    public RestaurantResponse activateRestaurant(
            @PathVariable String restaurantId,
            @RequestParam String ownerAccountId
    ) {
        RestaurantDocument restaurant = IRestaurantService.activateRestaurant(restaurantId, ownerAccountId);
        return catalogResponseMapper.toRestaurantResponse(restaurant);
    }
}