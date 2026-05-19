package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.BranchDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IBranchRepository extends MongoRepository<BranchDocument, String> {

    List<BranchDocument> findAllByRestaurantIdAndIsActiveTrue(String restaurantId);

    List<BranchDocument> findAllByRestaurantIdAndIsActiveTrueAndDeletedAtIsNull(String restaurantId);

    Optional<BranchDocument> findByRestaurantIdAndIsMainBranchTrueAndIsActiveTrue(String restaurantId);
}
