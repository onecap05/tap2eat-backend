package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.mappers.BranchMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.IBranchService;
import com.tap2eat.catalog.validators.BranchValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements IBranchService {

    private final IBranchRepository branchRepository;
    private final IRestaurantRepository IRestaurantRepository;
    private final BranchMapper branchMapper;

    @Override
    public BranchDocument createBranch(CreateBranchRequest request) {
        validateCreateRequest(request);
        validateRestaurantExists(request.restaurantId());

        BranchDocument branch = branchMapper.toDocument(request);
        BranchValidator.validate(branch);
        validateMainBranchRule(branch);

        return branchRepository.save(branch);
    }

    @Override
    public BranchDocument updateBranch(String restaurantId, String branchId, UpdateBranchRequest request) {
        if (isBlank(restaurantId) || isBlank(branchId) || request == null) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        validateRestaurantExists(restaurantId);

        BranchDocument branch = getBranchOrThrow(branchId);
        validateBranchOwnership(branch, restaurantId);

        branchMapper.updateDocument(branch, request);
        BranchValidator.validate(branch);
        validateMainBranchRule(branch);

        return branchRepository.save(branch);
    }

    @Override
    public BranchDocument getBranchById(String branchId) {
        if (isBlank(branchId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        return getBranchOrThrow(branchId);
    }

    @Override
    public List<BranchDocument> getBranchesByRestaurant(String restaurantId) {
        if (isBlank(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        validateRestaurantExists(restaurantId);
        return branchRepository.findAllByRestaurantIdAndIsActiveTrue(restaurantId);
    }

    @Override
    public BranchDocument deactivateBranch(String restaurantId, String branchId) {
        if (isBlank(restaurantId) || isBlank(branchId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        BranchDocument branch = getBranchOrThrow(branchId);
        validateBranchOwnership(branch, restaurantId);

        branch.setIsActive(Boolean.FALSE);
        branch.setDeletedAt(LocalDateTime.now());

        return branchRepository.save(branch);
    }

    @Override
    public BranchDocument activateBranch(String restaurantId, String branchId) {
        if (isBlank(restaurantId) || isBlank(branchId)) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }

        validateRestaurantExists(restaurantId);

        BranchDocument branch = getBranchOrThrow(branchId);
        validateBranchOwnership(branch, restaurantId);

        branch.setIsActive(Boolean.TRUE);
        branch.setDeletedAt(null);

        validateMainBranchRule(branch);

        return branchRepository.save(branch);
    }

    private void validateCreateRequest(CreateBranchRequest request) {
        if (request == null || isBlank(request.restaurantId())) {
            throw new CatalogValidationException(CatalogErrorCode.INVALID_BRANCH_DATA);
        }
    }

    private void validateRestaurantExists(String restaurantId) {
        if (!IRestaurantRepository.existsById(restaurantId)) {
            throw new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    private BranchDocument getBranchOrThrow(String branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new CatalogValidationException(CatalogErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validateBranchOwnership(BranchDocument branch, String restaurantId) {
        if (branch == null || !restaurantId.equals(branch.getRestaurantId())) {
            throw new CatalogValidationException(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
        }
    }

    private void validateMainBranchRule(BranchDocument branch) {
        if (!Boolean.TRUE.equals(branch.getIsMainBranch()) || !Boolean.TRUE.equals(branch.getIsActive())) {
            return;
        }

        branchRepository.findByRestaurantIdAndIsMainBranchTrueAndIsActiveTrue(branch.getRestaurantId())
                .ifPresent(existingMainBranch -> {
                    if (!Objects.equals(existingMainBranch.getId(), branch.getId())) {
                        throw new CatalogValidationException(CatalogErrorCode.MAIN_BRANCH_ALREADY_EXISTS);
                    }
                });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}