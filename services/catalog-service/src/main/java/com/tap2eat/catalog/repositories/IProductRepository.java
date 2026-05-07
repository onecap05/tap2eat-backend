package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IProductRepository extends MongoRepository<ProductDocument, String> {
    List<ProductDocument> findAllByRestaurantIdAndIsActiveTrue(String restaurantId);
    List<ProductDocument> findAllByCategoryIdAndIsActiveTrue(String categoryId);
}