package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.request.category.CreateCategoryRequest;
import com.tap2eat.catalog.dtos.request.category.UpdateCategoryRequest;
import com.tap2eat.catalog.dtos.request.product.*;
import com.tap2eat.catalog.models.documents.CategoryDocument;
import com.tap2eat.catalog.models.embedded.*;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CategoryMapper {

    public CategoryDocument toDocument(CreateCategoryRequest request) {
        if (request == null) {
            return null;
        }

        CategoryDocument document = new CategoryDocument();
        document.setRestaurantId(request.restaurantId());
        document.setName(request.name());
        document.setDescription(request.description());
        document.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        document.setImage(mapImage(request.image()));
        document.setAvailability(mapAvailability(request.availability()));
        document.setIsActive(Boolean.TRUE);

        return document;
    }

    public void updateDocument(CategoryDocument document, UpdateCategoryRequest request) {
        if (document == null || request == null) {
            return;
        }

        document.setName(request.name());
        document.setDescription(request.description());

        if (request.displayOrder() != null) {
            document.setDisplayOrder(request.displayOrder());
        }

        document.setImage(mapImage(request.image()));
        document.setAvailability(mapAvailability(request.availability()));
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