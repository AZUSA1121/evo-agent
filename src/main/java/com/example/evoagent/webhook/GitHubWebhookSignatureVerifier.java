package com.example.evoagent.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class GitHubWebhookSignatureVerifier {

    public void verify(String payload, String signatureHeader) {
        String secret = System.getenv("GITHUB_WEBHOOK_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing GITHUB_WEBHOOK_SECRET environment variable");
        }

        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            throw new IllegalStateException("Missing or invalid X-Hub-Signature-256 header");
        }

        String expected = "sha256=" + hmacSha256(payload, secret);
        boolean matched = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8)
        );
        if (!matched) {
            throw new IllegalStateException("GitHub webhook signature verification failed");
        }
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify GitHub webhook signature", e);
        }
    }
}
