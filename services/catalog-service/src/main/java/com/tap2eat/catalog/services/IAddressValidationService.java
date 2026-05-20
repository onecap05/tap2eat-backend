package com.tap2eat.catalog.services;

public interface IAddressValidationService {

    void validateMexicanAddress(
            String postalCode,
            String neighborhood,
            String city,
            String state,
            String country
    );
}