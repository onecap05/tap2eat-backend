package com.tap2eat.notification.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GmailConfigTest {

    @Test
    void gsonFactory_shouldReturnDefaultInstance() {
        GmailConfig config = new GmailConfig();

        GsonFactory gsonFactory = config.gsonFactory();

        assertThat(gsonFactory).isSameAs(GsonFactory.getDefaultInstance());
    }

    @Test
    void gmail_shouldBuildGmailClientWithConfiguredApplicationName() throws Exception {
        GmailConfig config = new GmailConfig();
        ReflectionTestUtils.setField(config, "gmailApplicationName", "Tap2Eat Test");
        GmailCredentialsProvider credentialsProvider = mock(GmailCredentialsProvider.class);
        when(credentialsProvider.getCredential()).thenReturn(mock(Credential.class));

        Gmail gmail = config.gmail(credentialsProvider, GsonFactory.getDefaultInstance());

        assertThat(gmail.getApplicationName()).isEqualTo("Tap2Eat Test");
    }
}
