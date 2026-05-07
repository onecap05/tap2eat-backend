package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.RestaurantDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IRestaurantRepository extends MongoRepository<RestaurantDocument, String> {
    Optional<RestaurantDocument> findByOwnerAccountIdAndIsActiveTrue(String ownerAccountId);
}