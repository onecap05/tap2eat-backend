package com.tap2eat.notification.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

@Component
public class GmailCredentialsProvider {

    private final String credentialsPath;
    private final String tokensDir;
    private final int localReceiverPort;

    public GmailCredentialsProvider(
            @Value("${gmail.credentials.path}") String credentialsPath,
            @Value("${gmail.tokens.dir}") String tokensDir,
            @Value("${gmail.oauth.local-receiver-port}") int localReceiverPort
    ) {
        this.credentialsPath = credentialsPath;
        this.tokensDir = tokensDir;
        this.localReceiverPort = localReceiverPort;
    }

    public Credential getCredential() throws Exception {
        File credentialsFile = new File(credentialsPath);
        File tokensDirectory = new File(tokensDir);

        if (!credentialsFile.exists() || !credentialsFile.isFile()) {
            throw new IllegalStateException("Gmail credentials file was not found at configured path.");
        }

        if (!tokensDirectory.exists()) {
            tokensDirectory.mkdirs();
        }

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                jsonFactory,
                new InputStreamReader(new FileInputStream(credentialsFile))
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                List.of(GmailScopes.GMAIL_SEND)
        )
                .setDataStoreFactory(new FileDataStoreFactory(tokensDirectory))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(localReceiverPort)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}