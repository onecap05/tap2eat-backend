package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.services.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    public BranchDocument createBranch(@RequestBody CreateBranchRequest request) {
        return branchService.createBranch(request);
    }

    @PutMapping("/{branchId}")
    public BranchDocument updateBranch(
            @PathVariable String branchId,
            @RequestParam String restaurantId,
            @RequestBody UpdateBranchRequest request
    ) {
        return branchService.updateBranch(restaurantId, branchId, request);
    }

    @GetMapping("/{branchId}")
    public BranchDocument getBranchById(@PathVariable String branchId) {
        return branchService.getBranchById(branchId);
    }

    @GetMapping("/restaurant/{restaurantId}")
    public List<BranchDocument> getBranchesByRestaurant(@PathVariable String restaurantId) {
        return branchService.getBranchesByRestaurant(restaurantId);
    }

    @PatchMapping("/{branchId}/deactivate")
    public BranchDocument deactivateBranch(
            @PathVariable String branchId,
            @RequestParam String restaurantId
    ) {
        return branchService.deactivateBranch(restaurantId, branchId);
    }

    @PatchMapping("/{branchId}/activate")
    public BranchDocument activateBranch(
            @PathVariable String branchId,
            @RequestParam String restaurantId
    ) {
        return branchService.activateBranch(restaurantId, branchId);
    }
}