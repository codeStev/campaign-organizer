package com.campaignorganizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Encrypts TOTP secrets at rest (ADR-0111) — a secret is directly usable to generate valid
 * codes, so it needs real encryption, not just hashing like a password. {@code Encryptors.delux}
 * is Spring Security Crypto's standard AES-256-GCM {@link TextEncryptor} (PBKDF2 key
 * derivation, a fresh random IV per call — not {@code Encryptors.stronger}, which returns the
 * lower-level {@code BytesEncryptor} this wraps rather than a String-in/String-out one).
 */
@Configuration
public class MfaEncryptionConfig {

    @Bean
    public TextEncryptor totpSecretEncryptor(AppProperties properties) {
        return Encryptors.delux(properties.mfa().encryptionKey(), properties.mfa().encryptionSalt());
    }
}
