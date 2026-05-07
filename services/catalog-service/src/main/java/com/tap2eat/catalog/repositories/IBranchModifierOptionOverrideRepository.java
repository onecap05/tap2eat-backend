package com.tap2eat.catalog.repositories;

import com.tap2eat.catalog.models.documents.BranchModifierOptionOverrideDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IBranchModifierOptionOverrideRepository extends MongoRepository<BranchModifierOptionOverrideDocument, String> {
    Optional<BranchModifierOptionOverrideDocument> findByBranchIdAndModifierOptionIdAndIsActiveTrue(String branchId, String modifierOptionId);
    List<BranchModifierOptionOverrideDocument> findAllByBranchIdAndIsActiveTrue(String branchId);
}