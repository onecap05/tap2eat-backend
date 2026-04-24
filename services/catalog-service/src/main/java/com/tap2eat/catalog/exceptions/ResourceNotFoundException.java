package com.tap2eat.catalog.exceptions;

public class ResourceNotFoundException extends CatalogException {

    public ResourceNotFoundException() {
        super(CatalogErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(CatalogErrorCode.RESOURCE_NOT_FOUND, message);
    }
}