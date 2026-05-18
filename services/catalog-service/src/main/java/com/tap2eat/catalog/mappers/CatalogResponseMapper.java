package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.response.branch.BranchResponse;
import com.tap2eat.catalog.dtos.response.category.CategoryResponse;
import com.tap2eat.catalog.dtos.response.common.*;
import com.tap2eat.catalog.dtos.response.product.ProductResponse;
import com.tap2eat.catalog.dtos.response.restaurant.RestaurantResponse;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.documents.RestaurantDocument;
import com.tap2eat.catalog.models.embedded.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CatalogResponseMapper {

    public RestaurantResponse toRestaurantResponse(RestaurantDocument document) {
        if (document == null) {
            return null;
        }

        return new RestaurantResponse(
                document.getId(),
                document.getOwnerAccountId(),
                document.getName(),
                document.getDescription(),
                toImageMetadataResponse(document.getLogo()),
                document.getIsActive(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getDeletedAt()
        );
    }

    public BranchResponse toBranchResponse(BranchDocument document) {
        if (document == null) {
            return null;
        }

        return new BranchResponse(
                document.getId(),
                document.getRestaurantId(),
                document.getName(),
                document.getPhoneNumber(),
                document.getFormattedAddress(),
                document.getStreet(),
                document.getExteriorNumber(),
                document.getInteriorNumber(),
                document.getNeighborhood(),
                document.getCity(),
                document.getState(),
                document.getPostalCode(),
                document.getCountry(),
                document.getAddressReference(),
                document.getLatitude(),
                document.getLongitude(),
                document.getGooglePlaceId(),
                toAvailabilityConfigResponse(document.getAvailability()),
                document.getIsMainBranch(),
                document.getIsActive(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getDeletedAt()
        );
    }

    public CategoryResponse toCategoryResponse(CategoryDocument document) {
        if (document == null) {
            return null;
        }

        return new CategoryResponse(
                document.getId(),
                document.getRestaurantId(),
                document.getName(),
                document.getDescription(),
                document.getDisplayOrder(),
                toImageMetadataResponse(document.getImage()),
                toAvailabilityConfigResponse(document.getAvailability()),
                document.getIsActive(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getDeletedAt()
        );
    }

    public ProductResponse toProductResponse(ProductDocument document) {
        if (document == null) {
            return null;
        }

        return new ProductResponse(
                document.getId(),
                document.getRestaurantId(),
                document.getCategoryId(),
                document.getName(),
                document.getDescription(),
                document.getProductType(),
                document.getPrice(),
                toImageMetadataResponse(document.getImage()),
                document.getDisplayOrder(),
                document.getFeatured(),
                toAvailabilityConfigResponse(document.getAvailability()),
                document.getIsActive(),
                safeList(document.getTags()),
                safeList(document.getDietaryFlags()),
                safeList(document.getAllergens()),
                toModifierGroupResponses(document.getModifierGroups()),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getDeletedAt()
        );
    }

    public List<RestaurantResponse> toRestaurantResponses(List<RestaurantDocument> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }

        return documents.stream()
                .map(this::toRestaurantResponse)
                .toList();
    }

    public List<BranchResponse> toBranchResponses(List<BranchDocument> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }

        return documents.stream()
                .map(this::toBranchResponse)
                .toList();
    }

    public List<CategoryResponse> toCategoryResponses(List<CategoryDocument> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }

        return documents.stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    public List<ProductResponse> toProductResponses(List<ProductDocument> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }

        return documents.stream()
                .map(this::toProductResponse)
                .toList();
    }

    private ImageMetadataResponse toImageMetadataResponse(ImageMetadata image) {
        if (image == null) {
            return null;
        }

        return new ImageMetadataResponse(
                image.getUrl(),
                image.getObjectKey(),
                image.getProvider(),
                image.getContentType(),
                image.getSize(),
                image.getUploadedAt()
        );
    }

    private AvailabilityConfigResponse toAvailabilityConfigResponse(AvailabilityConfig availability) {
        if (availability == null) {
            return null;
        }

        return new AvailabilityConfigResponse(
                availability.getStatus(),
                availability.getTemporaryReason(),
                availability.getTemporaryReasonDetail(),
                toDailyAvailabilityResponses(availability.getWeeklySchedule())
        );
    }

    private List<DailyAvailabilityResponse> toDailyAvailabilityResponses(List<DailyAvailability> weeklySchedule) {
        if (weeklySchedule == null) {
            return Collections.emptyList();
        }

        return weeklySchedule.stream()
                .map(this::toDailyAvailabilityResponse)
                .toList();
    }

    private DailyAvailabilityResponse toDailyAvailabilityResponse(DailyAvailability dailyAvailability) {
        if (dailyAvailability == null) {
            return null;
        }

        return new DailyAvailabilityResponse(
                dailyAvailability.getDayOfWeek(),
                dailyAvailability.getEnabled(),
                toTimeRangeResponses(dailyAvailability.getTimeRanges())
        );
    }

    private List<TimeRangeResponse> toTimeRangeResponses(List<TimeRange> timeRanges) {
        if (timeRanges == null) {
            return Collections.emptyList();
        }

        return timeRanges.stream()
                .map(this::toTimeRangeResponse)
                .toList();
    }

    private TimeRangeResponse toTimeRangeResponse(TimeRange timeRange) {
        if (timeRange == null) {
            return null;
        }

        return new TimeRangeResponse(
                timeRange.getStartTime(),
                timeRange.getEndTime()
        );
    }

    private List<ModifierGroupResponse> toModifierGroupResponses(List<ModifierGroup> groups) {
        if (groups == null) {
            return Collections.emptyList();
        }

        return groups.stream()
                .map(this::toModifierGroupResponse)
                .toList();
    }

    private ModifierGroupResponse toModifierGroupResponse(ModifierGroup group) {
        if (group == null) {
            return null;
        }

        return new ModifierGroupResponse(
                group.getId(),
                group.getName(),
                group.getSelectionType(),
                group.getRequired(),
                group.getMinSelections(),
                group.getMaxSelections(),
                group.getDisplayOrder(),
                group.getIsActive(),
                toModifierOptionResponses(group.getOptions())
        );
    }

    private List<ModifierOptionResponse> toModifierOptionResponses(List<ModifierOption> options) {
        if (options == null) {
            return Collections.emptyList();
        }

        return options.stream()
                .map(this::toModifierOptionResponse)
                .toList();
    }

    private ModifierOptionResponse toModifierOptionResponse(ModifierOption option) {
        if (option == null) {
            return null;
        }

        return new ModifierOptionResponse(
                option.getId(),
                option.getName(),
                option.getAdditionalPrice(),
                option.getIsActive(),
                option.getDisplayOrder()
        );
    }

    private List<String> safeList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values;
    }
}