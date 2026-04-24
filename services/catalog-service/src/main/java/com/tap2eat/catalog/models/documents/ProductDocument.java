package com.tap2eat.catalog.models.documents;

import com.tap2eat.catalog.models.embedded.AvailabilityConfig;
import com.tap2eat.catalog.models.embedded.ImageMetadata;
import com.tap2eat.catalog.models.embedded.ModifierGroup;
import com.tap2eat.catalog.models.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "products")
public class ProductDocument extends BaseDocument {

    private String restaurantId;
    private String categoryId;
    private String name;
    private String description;
    private ProductType productType;
    private BigDecimal price;
    private ImageMetadata image;
    private Integer displayOrder;
    private Boolean featured = false;
    private AvailabilityConfig availability;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<String> dietaryFlags = new ArrayList<>();

    @Builder.Default
    private List<String> allergens = new ArrayList<>();

    @Builder.Default
    private List<ModifierGroup> modifierGroups = new ArrayList<>();
}