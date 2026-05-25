package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface IProductRepository extends MongoRepository<ProductDocument, String> {
    List<ProductDocument> findAllByRestaurantIdAndIsActiveTrue(String restaurantId);
    List<ProductDocument> findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(String restaurantId);
    List<ProductDocument> findAllByIsActiveTrueAndDeletedAtIsNull();
    List<ProductDocument> findAllByCategoryIdAndIsActiveTrue(String categoryId);
    boolean existsByCategoryIdAndIsActiveTrue(String categoryId);
    java.util.Optional<ProductDocument> findByIdAndIsActiveTrueAndDeletedAtIsNull(String id);
}
