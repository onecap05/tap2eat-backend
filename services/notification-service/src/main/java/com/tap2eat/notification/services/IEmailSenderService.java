package com.tap2eat.notification.services;

public interface IEmailSenderService {
    void sendVerificationEmail(String to, String code);
}