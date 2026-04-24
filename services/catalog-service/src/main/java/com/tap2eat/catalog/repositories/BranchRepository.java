package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.BranchDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends MongoRepository<BranchDocument, String> {

    List<BranchDocument> findAllByRestaurantIdAndIsActiveTrue(String restaurantId);

    Optional<BranchDocument> findByRestaurantIdAndIsMainBranchTrueAndIsActiveTrue(String restaurantId);
}