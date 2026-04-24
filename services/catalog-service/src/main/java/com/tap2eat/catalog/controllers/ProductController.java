package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ProductDocument createProduct(@RequestBody CreateProductRequest request) {
        System.out.println("REQUEST: " + request);
        System.out.println("restaurantId: " + request.restaurantId());
        System.out.println("categoryId: " + request.categoryId());
        System.out.println("productType: " + request.productType());
        System.out.println("provider: " + (request.image() != null ? request.image().provider() : null));
        return productService.createProduct(request);
    }

    @PutMapping("/{productId}")
    public ProductDocument updateProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId,
            @RequestBody UpdateProductRequest request
    ) {
        return productService.updateProduct(restaurantId, productId, request);
    }

    @GetMapping("/{productId}")
    public ProductDocument getProductById(@PathVariable String productId) {
        return productService.getProductById(productId);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<ProductDocument> getProductsByRestaurant(@PathVariable String restaurantId) {
        return productService.getProductsByRestaurant(restaurantId);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductDocument> getProductsByCategory(@PathVariable String categoryId) {
        return productService.getProductsByCategory(categoryId);
    }

    @PatchMapping("/{productId}/deactivate")
    public ProductDocument deactivateProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId
    ) {
        return productService.deactivateProduct(restaurantId, productId);
    }

    @PatchMapping("/{productId}/activate")
    public ProductDocument activateProduct(
            @PathVariable String productId,
            @RequestParam String restaurantId
    ) {
        return productService.activateProduct(restaurantId, productId);
    }
}