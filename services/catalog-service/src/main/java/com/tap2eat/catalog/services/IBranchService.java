package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.models.documents.BranchDocument;

import java.util.List;

public interface IBranchService {

    BranchDocument createBranch(CreateBranchRequest request);

    BranchDocument updateBranch(String restaurantId, String branchId, UpdateBranchRequest request);

    BranchDocument getBranchById(String branchId);

    List<BranchDocument> getBranchesByRestaurant(String restaurantId);

    BranchDocument deactivateBranch(String restaurantId, String branchId);

    BranchDocument activateBranch(String restaurantId, String branchId);
}