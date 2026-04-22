package com.tap2eat.catalog.models.documents;

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
@Document(collection = "branch_modifier_option_overrides")
public class BranchModifierOptionOverrideDocument extends BaseDocument {

    private String branchId;
    private String productId;
    private String modifierGroupId;
    private String modifierOptionId;
    private AvailabilityStatus status;
    private String reason;
}