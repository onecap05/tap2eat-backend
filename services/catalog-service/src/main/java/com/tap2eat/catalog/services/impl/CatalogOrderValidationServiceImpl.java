package com.tap2eat.catalog.services.impl;

import com.tap2eat.catalog.dtos.request.internal.ValidateOrderItemRequest;
import com.tap2eat.catalog.dtos.request.internal.ValidateOrderRequest;
import com.tap2eat.catalog.dtos.response.internal.ValidateOrderResponse;
import com.tap2eat.catalog.dtos.response.internal.ValidatedModifierResponse;
import com.tap2eat.catalog.dtos.response.internal.ValidatedOrderItemResponse;
import com.tap2eat.catalog.exceptions.CatalogErrorCode;
import com.tap2eat.catalog.exceptions.CatalogValidationException;
import com.tap2eat.catalog.models.documents.BaseDocument;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.embedded.ModifierOption;
import com.tap2eat.catalog.repositories.IBranchRepository;
import com.tap2eat.catalog.repositories.ICategoryRepository;
import com.tap2eat.catalog.repositories.IProductRepository;
import com.tap2eat.catalog.repositories.IRestaurantRepository;
import com.tap2eat.catalog.services.IAvailabilityEvaluator;
import com.tap2eat.catalog.services.ICatalogOrderValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CatalogOrderValidationServiceImpl implements ICatalogOrderValidationService {

    private final IRestaurantRepository restaurantRepository;
    private final IBranchRepository branchRepository;
    private final ICategoryRepository categoryRepository;
    private final IProductRepository productRepository;
    private final IAvailabilityEvaluator availabilityEvaluator;

    @Override
    public ValidateOrderResponse validateOrder(ValidateOrderRequest request) {
        validateRequest(request);

        RestaurantDocument restaurant = getActiveRestaurant(request.restaurantId());
        BranchDocument branch = getActiveBranch(request.branchId());
        validateBranchBelongsToRestaurant(branch, restaurant.getId());

        if (!availabilityEvaluator.isBranchOpen(branch)) {
            throw invalidOrder("Branch is not open.");
        }

        List<ValidatedOrderItemResponse> items = request.items().stream()
                .map(item -> validateItem(item, restaurant.getId()))
                .toList();

        BigDecimal subtotal = items.stream()
                .map(ValidatedOrderItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ValidateOrderResponse(restaurant.getId(), branch.getId(), true, items, subtotal, subtotal);
    }

    private void validateRequest(ValidateOrderRequest request) {
        if (request == null
                || isBlank(request.restaurantId())
                || isBlank(request.branchId())
                || request.items() == null
                || request.items().isEmpty()) {
            throw invalidOrder("Order validation request is incomplete.");
        }
    }

    private RestaurantDocument getActiveRestaurant(String restaurantId) {
        return restaurantRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> invalidOrder("Restaurant does not exist or is inactive."));
    }

    private BranchDocument getActiveBranch(String branchId) {
        return branchRepository.findById(branchId)
                .filter(this::isVisible)
                .orElseThrow(() -> invalidOrder("Branch does not exist or is inactive."));
    }

    private void validateBranchBelongsToRestaurant(BranchDocument branch, String restaurantId) {
        if (!Objects.equals(branch.getRestaurantId(), restaurantId)) {
            throw invalidOrder("Branch does not belong to restaurant.");
        }
    }

    private ValidatedOrderItemResponse validateItem(ValidateOrderItemRequest item, String restaurantId) {
        if (item == null || isBlank(item.productId()) || item.quantity() == null || item.quantity() <= 0) {
            throw invalidOrder("Order item is invalid.");
        }

        ProductDocument product = productRepository.findByIdAndIsActiveTrueAndDeletedAtIsNull(item.productId())
                .orElseThrow(() -> invalidOrder("Product does not exist or is inactive."));

        validateProductBelongsToRestaurant(product, restaurantId);
        validateProductAvailability(product);
        validateCategoryAvailability(product);

        List<ValidatedModifierResponse> modifiers = safeOptionIds(item).stream()
                .map(optionId -> validateModifier(product, optionId))
                .toList();

        BigDecimal modifiersTotal = modifiers.stream()
                .map(ValidatedModifierResponse::priceAdjustment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unitPrice = defaultMoney(product.getPrice());
        BigDecimal subtotal = unitPrice
                .add(modifiersTotal)
                .multiply(BigDecimal.valueOf(item.quantity()));

        return new ValidatedOrderItemResponse(
                product.getId(),
                product.getName(),
                item.quantity(),
                unitPrice,
                modifiers,
                subtotal
        );
    }

    private void validateProductBelongsToRestaurant(ProductDocument product, String restaurantId) {
        if (!Objects.equals(product.getRestaurantId(), restaurantId)) {
            throw invalidOrder("Product does not belong to restaurant.");
        }
    }

    private void validateProductAvailability(ProductDocument product) {
        if (!availabilityEvaluator.isProductAvailable(product)) {
            throw invalidOrder("Product is not available.");
        }
    }

    private void validateCategoryAvailability(ProductDocument product) {
        if (isBlank(product.getCategoryId())) {
            return;
        }

        CategoryDocument category = categoryRepository.findById(product.getCategoryId())
                .filter(categoryDocument -> Objects.equals(categoryDocument.getRestaurantId(), product.getRestaurantId()))
                .filter(this::isVisible)
                .orElseThrow(() -> invalidOrder("Product category does not exist or is inactive."));

        if (!availabilityEvaluator.isCategoryAvailable(category)) {
            throw invalidOrder("Product category is not available.");
        }
    }

    private ValidatedModifierResponse validateModifier(ProductDocument product, String optionId) {
        if (isBlank(optionId)) {
            throw invalidModifier("Modifier option is invalid.");
        }

        for (ModifierGroup group : safeGroups(product)) {
            if (!Boolean.TRUE.equals(group.getIsActive())) {
                continue;
            }

            for (ModifierOption option : safeOptions(group)) {
                if (Objects.equals(option.getId(), optionId) && Boolean.TRUE.equals(option.getIsActive())) {
                    return new ValidatedModifierResponse(
                            group.getId(),
                            group.getName(),
                            option.getId(),
                            option.getName(),
                            defaultMoney(option.getAdditionalPrice())
                    );
                }
            }
        }

        throw invalidModifier("Modifier option does not exist for product.");
    }

    private boolean isVisible(BaseDocument document) {
        return Boolean.TRUE.equals(document.getIsActive()) && document.getDeletedAt() == null;
    }

    private List<String> safeOptionIds(ValidateOrderItemRequest item) {
        return item.selectedModifierOptionIds() == null ? List.of() : item.selectedModifierOptionIds();
    }

    private List<ModifierGroup> safeGroups(ProductDocument product) {
        return product.getModifierGroups() == null ? List.of() : product.getModifierGroups();
    }

    private List<ModifierOption> safeOptions(ModifierGroup group) {
        return group.getOptions() == null ? List.of() : group.getOptions();
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private CatalogValidationException invalidOrder(String message) {
        return new CatalogValidationException(CatalogErrorCode.INVALID_PRODUCT_DATA, message);
    }

    private CatalogValidationException invalidModifier(String message) {
        return new CatalogValidationException(CatalogErrorCode.INVALID_MODIFIER_OPTION, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
