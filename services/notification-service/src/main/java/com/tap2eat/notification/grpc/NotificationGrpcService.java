package com.tap2eat.notification.grpc;

import com.tap2eat.api.grpc.NotificationServiceGrpc;
import com.tap2eat.api.grpc.SendVerificationEmailRequest;
import com.tap2eat.api.grpc.SendVerificationEmailResponse;
import com.tap2eat.notification.services.IEmailSenderService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

import java.util.Locale;

@GrpcService
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NotificationGrpcService.class);

    private final IEmailSenderService emailSenderService;
    private final MessageSource messageSource;

    public NotificationGrpcService(IEmailSenderService emailSenderService,
                                   MessageSource messageSource) {
        this.emailSenderService = emailSenderService;
        this.messageSource = messageSource;
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
                    .setMessage(getMessage("notification.grpc.verification.success"))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("Failed to send verification email to {}", request.getTo(), ex);

            SendVerificationEmailResponse response = SendVerificationEmailResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage(getMessage("notification.grpc.verification.failure"))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, Locale.getDefault());
    }
}