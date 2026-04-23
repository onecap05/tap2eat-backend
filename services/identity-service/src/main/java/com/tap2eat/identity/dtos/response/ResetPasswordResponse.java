package com.tap2eat.identity.dtos.response;

public class ResetPasswordResponse {

    private String message;

    public ResetPasswordResponse() {
    }

    public ResetPasswordResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}