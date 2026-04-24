package com.tap2eat.catalog.exceptions;

public class UnauthorizedCatalogAccessException extends CatalogException {

    public UnauthorizedCatalogAccessException() {
        super(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS);
    }

    public UnauthorizedCatalogAccessException(String message) {
        super(CatalogErrorCode.UNAUTHORIZED_CATALOG_ACCESS, message);
    }
}