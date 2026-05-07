package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.request.product.CreateProductRequest;
import com.tap2eat.catalog.dtos.request.product.UpdateProductRequest;
import com.tap2eat.catalog.models.documents.ProductDocument;

import java.util.List;

public interface IProductService {

    ProductDocument createProduct(CreateProductRequest request);

    ProductDocument updateProduct(String restaurantId, String productId, UpdateProductRequest request);

    ProductDocument getProductById(String productId);

    List<ProductDocument> getProductsByRestaurant(String restaurantId);

    List<ProductDocument> getProductsByCategory(String categoryId);

    ProductDocument deactivateProduct(String restaurantId, String productId);

    ProductDocument activateProduct(String restaurantId, String productId);
}