package com.tap2eat.identity.dtos.response;

public class ResendVerificationCodeResponse {

    private String message;

    public ResendVerificationCodeResponse() {
    }

    public ResendVerificationCodeResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}