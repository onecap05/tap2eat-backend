package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.BranchProductOverrideDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IBranchProductOverrideRepository extends MongoRepository<BranchProductOverrideDocument, String> {
    Optional<BranchProductOverrideDocument> findByBranchIdAndProductIdAndIsActiveTrue(String branchId, String productId);
    List<BranchProductOverrideDocument> findAllByBranchIdAndIsActiveTrue(String branchId);
}