package com.tap2eat.catalog.models.documents;

import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.enums.AvailabilityStatus;
import lombok.AllArgsConstructor;
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
@Document(collection = "branch_product_overrides")
public class BranchProductOverrideDocument extends BaseDocument {

    private String branchId;
    private String productId;
    private AvailabilityStatus status;
    private AvailabilityConfig overrideAvailability;
    private String reason;
}