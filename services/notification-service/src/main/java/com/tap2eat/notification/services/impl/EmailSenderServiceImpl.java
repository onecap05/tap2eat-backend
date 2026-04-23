package com.tap2eat.notification.services.impl;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.tap2eat.notification.services.IEmailSenderService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

@Service
public class EmailSenderServiceImpl implements IEmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(EmailSenderServiceImpl.class);

    private final Gmail gmail;

    @Value("${gmail.from}")
    private String from;

    public EmailSenderServiceImpl(Gmail gmail) {
        this.gmail = gmail;
    }

    @Override
    public void sendVerificationEmail(String to, String code) {
        try {
            log.info("Preparing Gmail API request to send verification email to {}", to);

            String subject = "Verifica tu cuenta en Tap2Eat";
            String body = """
                    Hola,

                    Tu código de verificación para Tap2Eat es: %s

                    Este código expira en 15 minutos.

                    Si no solicitaste este registro, puedes ignorar este mensaje.
                    """.formatted(code);

            MimeMessage email = createEmail(to, from, subject, body);
            Message message = createMessageWithEmail(email);

            gmail.users().messages().send("me", message).execute();

            log.info("Verification email sent successfully to {}", to);
        } catch (Exception ex) {
            log.error("Gmail API failed while sending email to {}", to, ex);
            throw new RuntimeException("Failed to send verification email.", ex);
        }
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);

        return email;
    }

    private Message createMessageWithEmail(MimeMessage email) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = com.google.api.client.util.Base64.encodeBase64URLSafeString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}