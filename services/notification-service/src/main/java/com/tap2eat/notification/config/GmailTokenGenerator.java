package com.tap2eat.notification.config;

public class GmailTokenGenerator {

    public static void main(String[] args) throws Exception {
        String credentialsPath = "C:\\Users\\Angel\\tap2eat-backend\\secrets\\google\\credentials.json";
        String tokensDir = "C:\\Users\\Angel\\tap2eat-backend\\secrets\\google\\tokens";

        GmailCredentialsProvider provider = new GmailCredentialsProvider(credentialsPath, tokensDir);
        provider.getCredential();

        System.out.println("OAuth token generated successfully.");
    }
}