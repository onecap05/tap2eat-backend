package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.request.product.*;
import com.tap2eat.catalog.models.documents.ProductDocument;
import com.tap2eat.catalog.models.embedded.*;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductMapper {

    public ProductDocument toDocument(CreateProductRequest request) {
        if (request == null) {
            return null;
        }

        ProductDocument document = new ProductDocument();
        document.setRestaurantId(request.restaurantId());
        document.setCategoryId(request.categoryId());
        document.setName(request.name());
        document.setDescription(request.description());
        document.setProductType(request.productType());
        document.setPrice(request.price());
        document.setImage(mapImage(request.image()));
        document.setModifierGroups(mapModifierGroups(request.modifierGroups()));
        document.setAvailability(mapAvailability(request.availability()));
        document.setIsActive(request.active() != null ? request.active() : Boolean.TRUE);
        document.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        document.setFeatured(request.featured() != null ? request.featured() : Boolean.FALSE);
        document.setTags(request.tags() != null ? request.tags() : Collections.emptyList());
        document.setDietaryFlags(request.dietaryFlags() != null ? request.dietaryFlags() : Collections.emptyList());
        document.setAllergens(request.allergens() != null ? request.allergens() : Collections.emptyList());

        return document;
    }

    public void updateDocument(ProductDocument document, UpdateProductRequest request) {
        if (document == null || request == null) {
            return;
        }

        document.setCategoryId(request.categoryId());
        document.setName(request.name());
        document.setDescription(request.description());
        document.setProductType(request.productType());
        document.setPrice(request.price());
        document.setImage(mapImage(request.image()));
        document.setModifierGroups(mapModifierGroups(request.modifierGroups()));
        document.setAvailability(mapAvailability(request.availability()));

        if (request.active() != null) {
            document.setIsActive(request.active());
        }

        if (request.displayOrder() != null) {
            document.setDisplayOrder(request.displayOrder());
        }

        if (request.featured() != null) {
            document.setFeatured(request.featured());
        }

        if (request.tags() != null) {
            document.setTags(request.tags());
        }

        if (request.dietaryFlags() != null) {
            document.setDietaryFlags(request.dietaryFlags());
        }

        if (request.allergens() != null) {
            document.setAllergens(request.allergens());
        }
    }

    private List<ModifierGroup> mapModifierGroups(List<ModifierGroupRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }

        return requests.stream()
                .map(this::mapModifierGroup)
                .toList();
    }

    private ModifierGroup mapModifierGroup(ModifierGroupRequest request) {
        if (request == null) {
            return null;
        }

        ModifierGroup group = new ModifierGroup();
        group.setId(resolveId(request.id()));
        group.setName(request.name());
        group.setSelectionType(request.selectionType());
        group.setMinSelections(request.minSelections() != null ? request.minSelections() : 0);
        group.setMaxSelections(request.maxSelections() != null ? request.maxSelections() : 1);
        group.setRequired(request.required() != null ? request.required() : Boolean.FALSE);
        group.setIsActive(request.active() != null ? request.active() : Boolean.TRUE);
        group.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        group.setOptions(mapModifierOptions(request.options()));

        return group;
    }

    private ModifierOption mapModifierOption(ModifierOptionRequest request) {
        if (request == null) {
            return null;
        }

        ModifierOption option = new ModifierOption();
        option.setId(resolveId(request.id()));
        option.setName(request.name());
        option.setAdditionalPrice(request.additionalPrice() != null ? request.additionalPrice() : java.math.BigDecimal.ZERO);
        option.setIsActive(request.active() != null ? request.active() : Boolean.TRUE);
        option.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);

        return option;
    }

    private String resolveId(String id) {
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }

        return id;
    }

    private List<ModifierOption> mapModifierOptions(List<ModifierOptionRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }

        return requests.stream()
                .map(this::mapModifierOption)
                .toList();
    }



    private ImageMetadata mapImage(ImageMetadataRequest request) {
        if (request == null) {
            return null;
        }

        ImageMetadata image = new ImageMetadata();
        image.setUrl(request.url());
        image.setObjectKey(request.objectKey());
        image.setProvider(request.provider());

        return image;
    }

    private AvailabilityConfig mapAvailability(AvailabilityConfigRequest request) {
        if (request == null) {
            return null;
        }

        AvailabilityConfig availability = new AvailabilityConfig();
        availability.setStatus(request.status() != null
                ? request.status()
                : AvailabilityStatus.AVAILABLE);
        availability.setTemporaryReason(request.temporaryReason());
        availability.setTemporaryReasonDetail(request.temporaryReasonDetail());
        availability.setWeeklySchedule(mapDailyAvailabilities(request.weeklySchedule()));

        return availability;
    }

    private List<DailyAvailability> mapDailyAvailabilities(List<DailyAvailabilityRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }

        return requests.stream()
                .map(this::mapDailyAvailability)
                .toList();
    }

    private DailyAvailability mapDailyAvailability(DailyAvailabilityRequest request) {
        if (request == null) {
            return null;
        }

        DailyAvailability dailyAvailability = new DailyAvailability();
        dailyAvailability.setDayOfWeek(request.dayOfWeek());
        dailyAvailability.setEnabled(request.enabled() != null ? request.enabled() : Boolean.FALSE);
        dailyAvailability.setTimeRanges(mapTimeRanges(request.timeRanges()));

        return dailyAvailability;
    }

    private List<TimeRange> mapTimeRanges(List<TimeRangeRequest> requests) {
        if (requests == null) {
            return Collections.emptyList();
        }

        return requests.stream()
                .map(this::mapTimeRange)
                .toList();
    }

    private TimeRange mapTimeRange(TimeRangeRequest request) {
        if (request == null) {
            return null;
        }

        TimeRange timeRange = new TimeRange();
        timeRange.setStartTime(request.startTime());
        timeRange.setEndTime(request.endTime());

        return timeRange;
    }
}