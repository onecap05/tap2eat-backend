package com.tap2eat.notification.grpc;

import com.tap2eat.api.grpc.NotificationServiceGrpc;
import com.tap2eat.api.grpc.SendVerificationEmailRequest;
import com.tap2eat.api.grpc.SendVerificationEmailResponse;
import com.tap2eat.notification.services.IEmailSenderService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NotificationGrpcService.class);

    private final IEmailSenderService emailSenderService;

    public NotificationGrpcService(IEmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

    @Override
    public void sendVerificationEmail(
            SendVerificationEmailRequest request,
            StreamObserver<SendVerificationEmailResponse> responseObserver
    ) {
        try {
            log.info("Received request to send verification email to {}", request.getTo());

            emailSenderService.sendVerificationEmail(request.getTo(), request.getCode());

            SendVerificationEmailResponse response = SendVerificationEmailResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Verification email sent successfully.")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("Failed to send verification email to {}", request.getTo(), ex);

            SendVerificationEmailResponse response = SendVerificationEmailResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to send verification email: " + ex.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}