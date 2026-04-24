package com.tap2eat.identity.exceptions;

public class InvalidEmailVerificationCodeException extends RuntimeException {

    public InvalidEmailVerificationCodeException(String message) {
        super(message);
    }
}