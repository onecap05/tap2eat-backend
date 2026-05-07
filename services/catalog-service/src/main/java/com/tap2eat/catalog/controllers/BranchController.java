package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.dtos.response.branch.BranchResponse;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.services.IBranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final IBranchService IBranchService;
    private final CatalogResponseMapper catalogResponseMapper;

    @PostMapping
    public BranchResponse createBranch(@RequestBody CreateBranchRequest request) {
        BranchDocument branch = IBranchService.createBranch(request);
        return catalogResponseMapper.toBranchResponse(branch);
    }

    @PutMapping("/{branchId}")
    public BranchResponse updateBranch(
            @PathVariable String branchId,
            @RequestParam String restaurantId,
            @RequestBody UpdateBranchRequest request
    ) {
        BranchDocument branch = IBranchService.updateBranch(restaurantId, branchId, request);
        return catalogResponseMapper.toBranchResponse(branch);
    }

    @GetMapping("/{branchId}")
    public BranchResponse getBranchById(@PathVariable String branchId) {
        BranchDocument branch = IBranchService.getBranchById(branchId);
        return catalogResponseMapper.toBranchResponse(branch);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<BranchResponse> getBranchesByRestaurant(@PathVariable String restaurantId) {
        List<BranchDocument> branches = IBranchService.getBranchesByRestaurant(restaurantId);
        return catalogResponseMapper.toBranchResponses(branches);
    }

    @PatchMapping("/{branchId}/deactivate")
    public BranchResponse deactivateBranch(
            @PathVariable String branchId,
            @RequestParam String restaurantId
    ) {
        BranchDocument branch = IBranchService.deactivateBranch(restaurantId, branchId);
        return catalogResponseMapper.toBranchResponse(branch);
    }

    @PatchMapping("/{branchId}/activate")
    public BranchResponse activateBranch(
            @PathVariable String branchId,
            @RequestParam String restaurantId
    ) {
        BranchDocument branch = IBranchService.activateBranch(restaurantId, branchId);
        return catalogResponseMapper.toBranchResponse(branch);
    }
}