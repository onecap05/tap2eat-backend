package com.tap2eat.catalog.exceptions;

public class CatalogException extends RuntimeException {

    private final CatalogErrorCode errorCode;

    public CatalogException(CatalogErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public CatalogException(CatalogErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CatalogErrorCode getErrorCode() {
        return errorCode;
    }
}