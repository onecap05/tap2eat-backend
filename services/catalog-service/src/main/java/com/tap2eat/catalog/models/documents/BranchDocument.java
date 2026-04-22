package com.tap2eat.catalog.models.documents;

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
@Document(collection = "branches")
public class BranchDocument extends BaseDocument {

    private String restaurantId;
    private String name;
    private String address;
    private String phone;
    private Double latitude;
    private Double longitude;
}