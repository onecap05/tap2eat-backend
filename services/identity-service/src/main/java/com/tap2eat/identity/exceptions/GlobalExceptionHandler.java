package com.tap2eat.identity.exceptions;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse response = new ErrorResponse(
                getMessage("auth.validation.failed"),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRole(InvalidRoleException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ErrorResponse> handleWeakPassword(WeakPasswordException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<ErrorResponse> handleInactiveAccount(InactiveAccountException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidEmailVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmailVerificationCode(InvalidEmailVerificationCodeException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPasswordResetCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPasswordResetCode(InvalidPasswordResetCodeException ex) {
        return buildMessageResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        logger.error("Unexpected error handled by identity-service", ex);
        return buildMessageResponse(getMessage("auth.unexpected.error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildMessageResponse(String message, HttpStatus status) {
        ErrorResponse response = new ErrorResponse(message);
        return ResponseEntity.status(status).body(response);
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }
}