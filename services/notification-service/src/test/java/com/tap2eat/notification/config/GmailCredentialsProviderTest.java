package com.tap2eat.notification.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GmailCredentialsProviderTest {

    @Test
    void getCredential_whenCredentialsFileIsMissing_shouldThrowIllegalStateException() {
        GmailCredentialsProvider provider = new GmailCredentialsProvider(
                "missing/credentials.json",
                "target/test-google-tokens",
                0
        );

        assertThatThrownBy(provider::getCredential)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Gmail credentials file was not found at configured path.");
    }
}
