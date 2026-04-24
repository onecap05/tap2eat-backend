package com.tap2eat.catalog.models.documents;

import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "branches")
public class BranchDocument extends BaseDocument {

    private String restaurantId;
    private String name;
    private String phoneNumber;
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    private String googlePlaceId;
    private AvailabilityConfig availability;

    @Builder.Default
    private Boolean isMainBranch = false;
}