package com.tap2eat.identity.exceptions;

public class InvalidPasswordResetCodeException extends RuntimeException {

    public InvalidPasswordResetCodeException(String message) {
        super(message);
    }
}