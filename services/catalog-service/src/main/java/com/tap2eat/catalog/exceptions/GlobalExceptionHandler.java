package com.tap2eat.catalog.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CatalogException.class)
    public ResponseEntity<ErrorResponse> handleCatalogException(
            CatalogException exception,
            HttpServletRequest request
    ) {
        CatalogErrorCode errorCode = exception.getErrorCode();
        LOGGER.warn("Catalog exception handled. code={}, path={}, message={}",
                errorCode.getCode(),
                request.getRequestURI(),
                exception.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .code(errorCode.getCode())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        CatalogErrorCode errorCode = CatalogErrorCode.INTERNAL_ERROR;
        LOGGER.error("Unhandled catalog exception. path={}", request.getRequestURI(), exception);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getHttpStatus().value())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .code(errorCode.getCode())
                .message(errorCode.getDefaultMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }
}
