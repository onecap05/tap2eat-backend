package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.dtos.response.product.ProductResponse;
import com.tap2eat.catalog.dtos.request.product.PauseProductRequest;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.services.IProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService IProductService;
    private final CatalogResponseMapper catalogResponseMapper;

    @PostMapping
    public ProductResponse createProduct(@RequestBody CreateProductRequest request) {
        ProductDocument product = IProductService.createProduct(request);
        return catalogResponseMapper.toProductResponse(product);
    }

    @PutMapping("/{productId}")
    public ProductResponse updateProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId,
            @RequestBody UpdateProductRequest request
    ) {
        ProductDocument product = IProductService.updateProduct(restaurantId, productId, request);
        return catalogResponseMapper.toProductResponse(product);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProductById(@PathVariable String productId) {
        ProductDocument product = IProductService.getProductById(productId);
        return catalogResponseMapper.toProductResponse(product);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<ProductResponse> getProductsByRestaurant(@PathVariable String restaurantId) {
        List<ProductDocument> products = IProductService.getProductsByRestaurant(restaurantId);
        return catalogResponseMapper.toProductResponses(products);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductResponse> getProductsByCategory(@PathVariable String categoryId) {
        List<ProductDocument> products = IProductService.getProductsByCategory(categoryId);
        return catalogResponseMapper.toProductResponses(products);
    }

    @PatchMapping("/{productId}/deactivate")
    public ProductResponse deactivateProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId
    ) {
        ProductDocument product = IProductService.deactivateProduct(restaurantId, productId);
        return catalogResponseMapper.toProductResponse(product);
    }

    @PatchMapping("/{productId}/pause")
    public ProductResponse pauseProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId,
            @RequestBody PauseProductRequest request
    ) {
        ProductDocument product = IProductService.pauseProduct(restaurantId, productId, request);
        return catalogResponseMapper.toProductResponse(product);
    }

    @PatchMapping("/{productId}/resume")
    public ProductResponse resumeProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId
    ) {
        ProductDocument product = IProductService.resumeProduct(restaurantId, productId);
        return catalogResponseMapper.toProductResponse(product);
    }

    @PatchMapping("/{productId}/activate")
    public ProductResponse activateProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId
    ) {
        ProductDocument product = IProductService.activateProduct(restaurantId, productId);
        return catalogResponseMapper.toProductResponse(product);
    }
}