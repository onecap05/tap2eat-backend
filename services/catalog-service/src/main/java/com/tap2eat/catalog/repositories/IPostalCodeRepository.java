package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.PostalCodeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IPostalCodeRepository extends MongoRepository<PostalCodeDocument, String> {

    List<PostalCodeDocument> findAllByPostalCode(String postalCode);

    Optional<PostalCodeDocument> findFirstByPostalCodeAndNeighborhoodIgnoreCase(
            String postalCode,
            String neighborhood
    );

    boolean existsByPostalCode(String postalCode);
}