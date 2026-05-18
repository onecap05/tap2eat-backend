package com.tap2eat.catalog.mappers;

import com.tap2eat.catalog.dtos.request.branch.CreateBranchRequest;
import com.tap2eat.catalog.dtos.request.branch.UpdateBranchRequest;
import com.tap2eat.catalog.dtos.request.product.AvailabilityConfigRequest;
import com.tap2eat.catalog.dtos.request.product.DailyAvailabilityRequest;
import com.tap2eat.catalog.dtos.request.product.TimeRangeRequest;
import com.tap2eat.catalog.models.documents.BranchDocument;
import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.DailyAvailability;
import com.tap2eat.catalog.models.embedded.TimeRange;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BranchMapper {

    public BranchDocument toDocument(CreateBranchRequest request) {
        if (request == null) {
            return null;
        }

        BranchDocument document = new BranchDocument();
        document.setRestaurantId(request.restaurantId());
        document.setName(request.name());
        document.setPhoneNumber(request.phoneNumber());
        document.setFormattedAddress(request.formattedAddress());
        document.setStreet(request.street());
        document.setExteriorNumber(request.exteriorNumber());
        document.setInteriorNumber(request.interiorNumber());
        document.setNeighborhood(request.neighborhood());
        document.setCity(request.city());
        document.setState(request.state());
        document.setPostalCode(request.postalCode());
        document.setCountry(request.country());
        document.setAddressReference(request.addressReference());
        document.setLatitude(request.latitude());
        document.setLongitude(request.longitude());
        document.setGooglePlaceId(request.googlePlaceId());
        document.setAvailability(mapAvailability(request.availability()));
        document.setIsMainBranch(request.isMainBranch() != null ? request.isMainBranch() : Boolean.FALSE);
        document.setIsActive(Boolean.TRUE);

        return document;
    }

    public void updateDocument(BranchDocument document, UpdateBranchRequest request) {
        if (document == null || request == null) {
            return;
        }

        document.setName(request.name());
        document.setPhoneNumber(request.phoneNumber());
        document.setFormattedAddress(request.formattedAddress());
        document.setStreet(request.street());
        document.setExteriorNumber(request.exteriorNumber());
        document.setInteriorNumber(request.interiorNumber());
        document.setNeighborhood(request.neighborhood());
        document.setCity(request.city());
        document.setState(request.state());
        document.setPostalCode(request.postalCode());
        document.setCountry(request.country());
        document.setAddressReference(request.addressReference());
        document.setLatitude(request.latitude());
        document.setLongitude(request.longitude());
        document.setGooglePlaceId(request.googlePlaceId());
        document.setAvailability(mapAvailability(request.availability()));

        if (request.isMainBranch() != null) {
            document.setIsMainBranch(request.isMainBranch());
        }
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