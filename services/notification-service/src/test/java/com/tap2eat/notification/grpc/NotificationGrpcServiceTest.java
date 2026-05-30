package com.tap2eat.notification.grpc;

import com.tap2eat.api.grpc.SendVerificationEmailRequest;
import com.tap2eat.api.grpc.SendVerificationEmailResponse;
import com.tap2eat.notification.services.IEmailSenderService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationGrpcServiceTest {

    @Mock
    private IEmailSenderService emailSenderService;

    @Mock
    private MessageSource messageSource;

    private NotificationGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new NotificationGrpcService(emailSenderService, messageSource);
    }

    @Test
    void sendVerificationEmail_whenEmailIsSent_shouldReturnSuccessResponse() {
        SendVerificationEmailRequest request = request();
        TestStreamObserver responseObserver = new TestStreamObserver();
        when(messageSource.getMessage("notification.grpc.verification.success", null, Locale.getDefault()))
                .thenReturn("Verification email sent successfully.");

        grpcService.sendVerificationEmail(request, responseObserver);

        verify(emailSenderService).sendVerificationEmail("user@tap2eat.local", "123456");
        assertThat(responseObserver.response().getSuccess()).isTrue();
        assertThat(responseObserver.response().getMessage()).isEqualTo("Verification email sent successfully.");
        assertThat(responseObserver.completed()).isTrue();
        assertThat(responseObserver.error()).isNull();
    }

    @Test
    void sendVerificationEmail_whenEmailServiceFails_shouldReturnFailureResponse() {
        SendVerificationEmailRequest request = request();
        TestStreamObserver responseObserver = new TestStreamObserver();
        doThrow(new RuntimeException("smtp unavailable"))
                .when(emailSenderService)
                .sendVerificationEmail("user@tap2eat.local", "123456");
        when(messageSource.getMessage("notification.grpc.verification.failure", null, Locale.getDefault()))
                .thenReturn("Failed to send verification email.");

        grpcService.sendVerificationEmail(request, responseObserver);

        assertThat(responseObserver.response().getSuccess()).isFalse();
        assertThat(responseObserver.response().getMessage()).isEqualTo("Failed to send verification email.");
        assertThat(responseObserver.completed()).isTrue();
        assertThat(responseObserver.error()).isNull();
    }

    private static SendVerificationEmailRequest request() {
        return SendVerificationEmailRequest.newBuilder()
                .setTo("user@tap2eat.local")
                .setCode("123456")
                .build();
    }

    private static class TestStreamObserver implements StreamObserver<SendVerificationEmailResponse> {

        private SendVerificationEmailResponse response;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(SendVerificationEmailResponse response) {
            this.response = response;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }

        private SendVerificationEmailResponse response() {
            return response;
        }

        private Throwable error() {
            return error;
        }

        private boolean completed() {
            return completed;
        }
    }
}
