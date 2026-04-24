package com.tap2eat.notification.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GmailConfig {

    @Value("${gmail.application-name}")
    private String gmailApplicationName;

    @Bean
    public GsonFactory gsonFactory() {
        return GsonFactory.getDefaultInstance();
    }

    @Bean
    public Gmail gmail(GmailCredentialsProvider gmailCredentialsProvider, GsonFactory gsonFactory) throws Exception {
        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                gsonFactory,
                gmailCredentialsProvider.getCredential()
        )
                .setApplicationName(gmailApplicationName)
                .build();
    }
}