package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.BranchDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BranchRepository extends MongoRepository<BranchDocument, String> {
    List<BranchDocument> findAllByRestaurantIdAndIsActiveTrue(String restaurantId);
}