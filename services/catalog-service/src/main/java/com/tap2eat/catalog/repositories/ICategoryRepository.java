package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.CategoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ICategoryRepository extends MongoRepository<CategoryDocument, String> {
    List<CategoryDocument> findAllByRestaurantIdAndIsActiveTrue(String restaurantId);
}