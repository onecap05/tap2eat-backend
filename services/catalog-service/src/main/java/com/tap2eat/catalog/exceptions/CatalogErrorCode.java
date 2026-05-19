package com.tap2eat.catalog.exceptions;

import org.springframework.http.HttpStatus;

public enum CatalogErrorCode {

    INVALID_MODIFIER_GROUP("CATALOG_001", "Invalid modifier group", HttpStatus.BAD_REQUEST),
    INVALID_MODIFIER_OPTION("CATALOG_002", "Invalid modifier option", HttpStatus.BAD_REQUEST),
    INVALID_TIME_RANGE("CATALOG_003", "Invalid time range", HttpStatus.BAD_REQUEST),
    INVALID_AVAILABILITY("CATALOG_004", "Invalid availability configuration", HttpStatus.BAD_REQUEST),
    INVALID_CATEGORY_DATA("CATALOG_005", "Invalid category data", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT_DATA("CATALOG_006", "Invalid product data", HttpStatus.BAD_REQUEST),
    INVALID_BRANCH_OVERRIDE("CATALOG_007", "Invalid branch override", HttpStatus.BAD_REQUEST),


    RESOURCE_NOT_FOUND("CATALOG_008", "Requested resource was not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED_CATALOG_ACCESS("CATALOG_009", "Unauthorized catalog access", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("CATALOG_010", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_RESTAURANT_DATA("CATALOG_011", "Invalid restaurant data", HttpStatus.BAD_REQUEST),
    RESTAURANT_ALREADY_EXISTS("CATALOG_012", "Restaurant already exists for this owner", HttpStatus.CONFLICT),
    INVALID_BRANCH_DATA("CATALOG_013", "Invalid branch data", HttpStatus.BAD_REQUEST),
    MAIN_BRANCH_ALREADY_EXISTS("CATALOG_014", "Main branch already exists for this restaurant", HttpStatus.CONFLICT),
    RESTAURANT_HAS_ACTIVE_BRANCHES("CATALOG_015", "Restaurant still has active branches", HttpStatus.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    CatalogErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}