package com.tap2eat.notification.services.impl;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.tap2eat.notification.exceptions.EmailSendingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderServiceImplTest {

    @Mock
    private Gmail gmail;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.Messages messages;

    @Mock
    private Gmail.Users.Messages.Send send;

    @Mock
    private MessageSource messageSource;

    private EmailSenderServiceImpl emailSenderService;

    @BeforeEach
    void setUp() {
        emailSenderService = new EmailSenderServiceImpl(gmail, messageSource);
        ReflectionTestUtils.setField(emailSenderService, "from", "noreply@tap2eat.local");
        ReflectionTestUtils.setField(emailSenderService, "verificationExpirationMinutes", 15);
    }

    @Test
    void sendVerificationEmail_shouldSendMessageThroughGmailApi() throws Exception {
        when(messageSource.getMessage("notification.email.verification.subject", new Object[]{}, Locale.getDefault()))
                .thenReturn("Verify your account");
        when(messageSource.getMessage(
                "notification.email.verification.body",
                new Object[]{"123456", 15},
                Locale.getDefault()
        )).thenReturn("Your verification code is 123456");
        when(gmail.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.send(eq("me"), any(Message.class))).thenReturn(send);
        when(send.execute()).thenReturn(new Message());

        emailSenderService.sendVerificationEmail("user@tap2eat.local", "123456");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messages).send(eq("me"), messageCaptor.capture());
        assertThat(messageCaptor.getValue().getRaw()).isNotBlank();
        verify(send).execute();
    }

    @Test
    void sendVerificationEmail_whenGmailApiFails_shouldThrowEmailSendingException() throws Exception {
        IOException gmailFailure = new IOException("gmail unavailable");
        when(messageSource.getMessage("notification.email.verification.subject", new Object[]{}, Locale.getDefault()))
                .thenReturn("Verify your account");
        when(messageSource.getMessage(
                "notification.email.verification.body",
                new Object[]{"123456", 15},
                Locale.getDefault()
        )).thenReturn("Your verification code is 123456");
        when(messageSource.getMessage(
                "notification.email.verification.send.error",
                new Object[]{},
                Locale.getDefault()
        )).thenReturn("Failed to send verification email.");
        when(gmail.users()).thenReturn(users);
        when(users.messages()).thenReturn(messages);
        when(messages.send(eq("me"), any(Message.class))).thenReturn(send);
        when(send.execute()).thenThrow(gmailFailure);

        assertThatThrownBy(() -> emailSenderService.sendVerificationEmail("user@tap2eat.local", "123456"))
                .isInstanceOf(EmailSendingException.class)
                .hasMessage("Failed to send verification email.")
                .hasCause(gmailFailure);
    }
}
