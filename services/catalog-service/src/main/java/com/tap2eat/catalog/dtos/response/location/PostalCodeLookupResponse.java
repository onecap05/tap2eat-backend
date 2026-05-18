package com.tap2eat.catalog.dtos.response.location;

import java.util.List;

public record PostalCodeLookupResponse(
        String postalCode,
        String city,
        String municipality,
        String state,
        String country,
        List<String> neighborhoods
) {
}