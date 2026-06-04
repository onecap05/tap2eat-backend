package com.tap2eat.identity.services;

import com.tap2eat.api.grpc.NotificationServiceGrpc;
import com.tap2eat.api.grpc.SendVerificationEmailRequest;
import com.tap2eat.api.grpc.SendVerificationEmailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationGrpcClientTest {

    @Mock
    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceBlockingStub;

    @Test
    void sendVerificationEmail_whenGrpcCallSucceeds_shouldSendExpectedRequest() {
        NotificationGrpcClient client = clientWithStub();
        when(notificationServiceBlockingStub.sendVerificationEmail(org.mockito.ArgumentMatchers.any()))
                .thenReturn(SendVerificationEmailResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("sent")
                        .build());

        client.sendVerificationEmail("user@example.com", "123456");

        ArgumentCaptor<SendVerificationEmailRequest> requestCaptor =
                ArgumentCaptor.forClass(SendVerificationEmailRequest.class);
        verify(notificationServiceBlockingStub).sendVerificationEmail(requestCaptor.capture());
        assertEquals("user@example.com", requestCaptor.getValue().getTo());
        assertEquals("123456", requestCaptor.getValue().getCode());
    }

    @Test
    void sendVerificationEmail_whenGrpcResponseFails_shouldThrowRuntimeException() {
        NotificationGrpcClient client = clientWithStub();
        when(notificationServiceBlockingStub.sendVerificationEmail(org.mockito.ArgumentMatchers.any()))
                .thenReturn(SendVerificationEmailResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("smtp unavailable")
                        .build());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> client.sendVerificationEmail("user@example.com", "123456"));

        assertEquals("Failed to send verification email: smtp unavailable", exception.getMessage());
    }

    private NotificationGrpcClient clientWithStub() {
        NotificationGrpcClient client = new NotificationGrpcClient();
        ReflectionTestUtils.setField(client, "notificationServiceBlockingStub", notificationServiceBlockingStub);
        return client;
    }
}
