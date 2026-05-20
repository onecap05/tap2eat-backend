package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.CatalogResponseMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.services.IBranchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BranchControllerTest {

    private final IBranchService service = mock(IBranchService.class);
    private final BranchController controller = new BranchController(service, new CatalogResponseMapper());

    @Test
    void branchEndpoints_shouldDelegateToServiceAndMapResponse() {
        BranchDocument branch = CatalogTestDataFactory.branch();
        when(service.createBranch(CatalogTestDataFactory.createBranchRequest())).thenReturn(branch);
        when(service.updateBranch(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.BRANCH_ID, CatalogTestDataFactory.updateBranchRequest())).thenReturn(branch);
        when(service.getBranchById(CatalogTestDataFactory.BRANCH_ID)).thenReturn(branch);
        when(service.getBranchesByRestaurant(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(List.of(branch));
        when(service.deactivateBranch(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.BRANCH_ID)).thenReturn(branch);
        when(service.activateBranch(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.BRANCH_ID)).thenReturn(branch);

        assertThat(controller.createBranch(CatalogTestDataFactory.createBranchRequest()).id()).isEqualTo(CatalogTestDataFactory.BRANCH_ID);
        assertThat(controller.updateBranch(CatalogTestDataFactory.BRANCH_ID, CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.updateBranchRequest()).id()).isEqualTo(CatalogTestDataFactory.BRANCH_ID);
        assertThat(controller.getBranchById(CatalogTestDataFactory.BRANCH_ID).id()).isEqualTo(CatalogTestDataFactory.BRANCH_ID);
        assertThat(controller.getBranchesByRestaurant(CatalogTestDataFactory.RESTAURANT_ID)).hasSize(1);
        assertThat(controller.deactivateBranch(CatalogTestDataFactory.BRANCH_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.BRANCH_ID);
        assertThat(controller.activateBranch(CatalogTestDataFactory.BRANCH_ID, CatalogTestDataFactory.RESTAURANT_ID).id()).isEqualTo(CatalogTestDataFactory.BRANCH_ID);
    }
}
