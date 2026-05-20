package com.tap2eat.catalog.models.documents;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "postal_codes")
public class PostalCodeDocument {

    @Id
    private String id;

    @Indexed
    private String postalCode;

    private String neighborhood;
    private String city;
    private String municipality;
    private String state;
    private String country;
}