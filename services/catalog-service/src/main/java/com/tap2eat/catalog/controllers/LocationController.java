package com.tap2eat.catalog.controllers;

import com.tap2eat.catalog.dtos.response.location.PostalCodeLookupResponse;
import com.tap2eat.catalog.services.IPostalCodeLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final IPostalCodeLookupService postalCodeLookupService;

    @GetMapping("/postal-codes/{postalCode}")
    public PostalCodeLookupResponse lookupByPostalCode(@PathVariable String postalCode) {
        return postalCodeLookupService.lookupByPostalCode(postalCode);
    }
}