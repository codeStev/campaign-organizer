package com.campaignorganizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Encrypts Foundry relay API keys at rest (ADR-0115) — a leaked key lets
 * someone else push arbitrary content into the account's self-hosted Foundry
 * session, so it needs real encryption, not just hashing. Uses its own
 * dedicated key/salt pair, distinct from {@link MfaEncryptionConfig}'s — one
 * secret type, one key, per this app's existing convention. Otherwise
 * identical to {@code MfaEncryptionConfig}: {@code Encryptors.delux} is
 * Spring Security Crypto's standard AES-256-GCM {@link TextEncryptor}.
 */
@Configuration
public class FoundryEncryptionConfig {

    @Bean
    public TextEncryptor foundryApiKeyEncryptor(AppProperties properties) {
        return Encryptors.delux(properties.foundry().encryptionKey(), properties.foundry().encryptionSalt());
    }
}
