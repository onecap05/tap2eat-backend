package com.tap2eat.catalog.services;

import com.tap2eat.catalog.dtos.response.location.PostalCodeLookupResponse;

public interface IPostalCodeLookupService {

    PostalCodeLookupResponse lookupByPostalCode(String postalCode);
}