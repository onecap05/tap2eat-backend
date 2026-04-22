package com.tap2eat.catalog.models.documents;

import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
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
@Document(collection = "categories")
public class CategoryDocument extends BaseDocument {

    private String restaurantId;
    private String name;
    private String description;
    private Integer displayOrder;
    private ImageMetadata image;
    private AvailabilityConfig baseAvailability;
}