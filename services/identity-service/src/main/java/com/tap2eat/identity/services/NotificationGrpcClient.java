package com.tap2eat.identity.services;

import com.tap2eat.api.grpc.NotificationServiceGrpc;
import com.tap2eat.api.grpc.SendVerificationEmailRequest;
import com.tap2eat.api.grpc.SendVerificationEmailResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class NotificationGrpcClient {

    @GrpcClient("notification-service")
    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceBlockingStub;

    public void sendVerificationEmail(String to, String code) {
        SendVerificationEmailRequest request = SendVerificationEmailRequest.newBuilder()
                .setTo(to)
                .setCode(code)
                .build();

        SendVerificationEmailResponse response = notificationServiceBlockingStub.sendVerificationEmail(request);

        if (!response.getSuccess()) {
            throw new RuntimeException("Failed to send verification email: " + response.getMessage());
        }
    }
}