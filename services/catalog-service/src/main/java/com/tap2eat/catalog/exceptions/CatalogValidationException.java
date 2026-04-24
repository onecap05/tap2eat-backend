package com.tap2eat.catalog.exceptions;

public class CatalogValidationException extends CatalogException {

    public CatalogValidationException(CatalogErrorCode errorCode) {
        super(errorCode);
    }

    public CatalogValidationException(CatalogErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}