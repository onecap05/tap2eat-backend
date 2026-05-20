package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.fixtures.CatalogTestDataFactory;
import com.tap2eat.catalog.mappers.BranchMapper;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.IAddressValidationService;
import com.tap2eat.catalog.services.ICatalogAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock
    private IBranchRepository branchRepository;

    @Mock
    private IRestaurantRepository restaurantRepository;

    @Mock
    private ICatalogAuthorizationService authorizationService;

    @Mock
    private IAddressValidationService addressValidationService;

    private BranchServiceImpl branchService;

    @BeforeEach
    void setUp() {
        branchService = new BranchServiceImpl(
                branchRepository,
                restaurantRepository,
                authorizationService,
                addressValidationService,
                new BranchMapper()
        );
    }

    @Test
    void createBranch_shouldCreateValidBranchWithCoordinates() {
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(branchRepository.save(any(BranchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchDocument branch = branchService.createBranch(CatalogTestDataFactory.createBranchRequest());

        assertThat(branch.getRestaurantId()).isEqualTo(CatalogTestDataFactory.RESTAURANT_ID);
        assertThat(branch.getLatitude()).isEqualTo(19.4326);
        assertThat(branch.getLongitude()).isEqualTo(-99.1332);
        assertThat(branch.getIsActive()).isTrue();
        verify(addressValidationService).validateMexicanAddress("06000", "Centro", "Ciudad de Mexico", "CDMX", "Mexico");
    }

    @Test
    void createBranch_shouldRejectInvalidRequestRestaurantOrCoordinates() {
        assertThatThrownBy(() -> branchService.createBranch(null)).isInstanceOf(CatalogValidationException.class);
        CreateBranchRequest blankRestaurant = new CreateBranchRequest(
                " ",
                "Centro",
                "+525512345678",
                "Address",
                "Street",
                "1",
                null,
                "Centro",
                "Ciudad de Mexico",
                "CDMX",
                "06000",
                "Mexico",
                null,
                19.0,
                -99.0,
                null,
                CatalogTestDataFactory.availabilityRequest(),
                Boolean.FALSE
        );
        assertThatThrownBy(() -> branchService.createBranch(blankRestaurant)).isInstanceOf(CatalogValidationException.class);

        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(false);
        assertThatThrownBy(() -> branchService.createBranch(CatalogTestDataFactory.createBranchRequest()))
                .isInstanceOf(CatalogValidationException.class);

        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        CreateBranchRequest invalidCoordinates = new CreateBranchRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                "Centro",
                "+525512345678",
                "Address",
                "Street",
                "1",
                null,
                "Centro",
                "Ciudad de Mexico",
                "CDMX",
                "06000",
                "Mexico",
                null,
                100.0,
                -99.0,
                null,
                CatalogTestDataFactory.availabilityRequest(),
                Boolean.FALSE
        );
        assertThatThrownBy(() -> branchService.createBranch(invalidCoordinates))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void createBranch_shouldRejectSecondActiveMainBranch() {
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(branchRepository.findByRestaurantIdAndIsMainBranchTrueAndIsActiveTrue(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.branch("existing-main", CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.openAvailability())));
        CreateBranchRequest request = new CreateBranchRequest(
                CatalogTestDataFactory.RESTAURANT_ID,
                "Centro",
                "+525512345678",
                "Address",
                "Street",
                "1",
                null,
                "Centro",
                "Ciudad de Mexico",
                "CDMX",
                "06000",
                "Mexico",
                null,
                19.0,
                -99.0,
                null,
                CatalogTestDataFactory.availabilityRequest(),
                Boolean.TRUE
        );

        assertThatThrownBy(() -> branchService.createBranch(request)).isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void getBranchesByRestaurant_shouldListOnlyActiveRepositoryResults() {
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(branchRepository.findAllByRestaurantIdAndIsActiveTrue(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(List.of(CatalogTestDataFactory.branch()));

        List<BranchDocument> branches = branchService.getBranchesByRestaurant(CatalogTestDataFactory.RESTAURANT_ID);

        assertThat(branches).hasSize(1);
        verify(authorizationService).validateCurrentAccountOwnsRestaurant(CatalogTestDataFactory.RESTAURANT_ID);
    }

    @Test
    void getBranchById_shouldReturnBranchAndValidateOwner() {
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.branch()));

        BranchDocument branch = branchService.getBranchById(CatalogTestDataFactory.BRANCH_ID);

        assertThat(branch.getId()).isEqualTo(CatalogTestDataFactory.BRANCH_ID);
        verify(authorizationService).validateCurrentAccountOwnsRestaurant(CatalogTestDataFactory.RESTAURANT_ID);
    }

    @Test
    void getBranchByIdAndList_shouldRejectBlankOrMissingResources() {
        assertThatThrownBy(() -> branchService.getBranchById(" "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> branchService.getBranchesByRestaurant(" "))
                .isInstanceOf(CatalogValidationException.class);
        when(branchRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> branchService.getBranchById("missing"))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void updateBranch_shouldUpdateAddressLocationAndAvailability() {
        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID))
                .thenReturn(Optional.of(CatalogTestDataFactory.branch()));
        when(branchRepository.findByRestaurantIdAndIsMainBranchTrueAndIsActiveTrue(CatalogTestDataFactory.RESTAURANT_ID))
                .thenReturn(Optional.empty());
        when(branchRepository.save(any(BranchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchDocument branch = branchService.updateBranch(
                CatalogTestDataFactory.RESTAURANT_ID,
                CatalogTestDataFactory.BRANCH_ID,
                CatalogTestDataFactory.updateBranchRequest()
        );

        assertThat(branch.getName()).isEqualTo("Roma");
        assertThat(branch.getFormattedAddress()).contains("Roma");
        assertThat(branch.getIsMainBranch()).isTrue();
    }

    @Test
    void updateBranch_shouldRejectWrongRestaurantOrInvalidInput() {
        assertThatThrownBy(() -> branchService.updateBranch(" ", CatalogTestDataFactory.BRANCH_ID, CatalogTestDataFactory.updateBranchRequest()))
                .isInstanceOf(CatalogValidationException.class);
        BranchDocument branch = CatalogTestDataFactory.branch();
        when(restaurantRepository.existsById("other-restaurant")).thenReturn(true);
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID)).thenReturn(Optional.of(branch));

        assertThatThrownBy(() -> branchService.updateBranch("other-restaurant", CatalogTestDataFactory.BRANCH_ID, CatalogTestDataFactory.updateBranchRequest()))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void deactivateAndActivateBranch_shouldSoftDeleteAndRestore() {
        BranchDocument branch = CatalogTestDataFactory.branch();
        when(branchRepository.findById(CatalogTestDataFactory.BRANCH_ID)).thenReturn(Optional.of(branch));
        when(branchRepository.save(any(BranchDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchDocument deleted = branchService.deactivateBranch(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.BRANCH_ID);

        assertThat(deleted.getIsActive()).isFalse();
        assertThat(deleted.getDeletedAt()).isNotNull();

        when(restaurantRepository.existsById(CatalogTestDataFactory.RESTAURANT_ID)).thenReturn(true);
        BranchDocument restored = branchService.activateBranch(CatalogTestDataFactory.RESTAURANT_ID, CatalogTestDataFactory.BRANCH_ID);

        assertThat(restored.getIsActive()).isTrue();
        assertThat(restored.getDeletedAt()).isNull();
    }

    @Test
    void deactivateAndActivateBranch_shouldRejectBlankInputs() {
        assertThatThrownBy(() -> branchService.deactivateBranch(" ", CatalogTestDataFactory.BRANCH_ID))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> branchService.deactivateBranch(CatalogTestDataFactory.RESTAURANT_ID, " "))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> branchService.activateBranch(" ", CatalogTestDataFactory.BRANCH_ID))
                .isInstanceOf(CatalogValidationException.class);
        assertThatThrownBy(() -> branchService.activateBranch(CatalogTestDataFactory.RESTAURANT_ID, " "))
                .isInstanceOf(CatalogValidationException.class);
    }

    @Test
    void branchWithoutSchedule_shouldBeOpenByAvailabilityEvaluatorCompatibility() {
        AvailabilityEvaluatorImpl evaluator = new AvailabilityEvaluatorImpl(java.time.Clock.systemDefaultZone());
        BranchDocument branch = CatalogTestDataFactory.branch();
        branch.setAvailability(null);

        assertThatCode(() -> assertThat(evaluator.isBranchOpen(branch)).isTrue()).doesNotThrowAnyException();
    }
}
