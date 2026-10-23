package com.campaignorganizer.accounts.adapter.account.out.apitoken;

import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenSecretPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** 256 bits from {@link SecureRandom}, hashed with SHA-256: the secret has full entropy, so no slow hash is needed. */
@Component
public class SecureRandomApiTokenSecretAdapter implements ApiTokenSecretPort {

    static final String PREFIX = "cot_";
    private static final int SECRET_BYTES = 32;
    private static final int DISPLAY_PREFIX_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    @Override
    public GeneratedSecret generate() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        String secret = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GeneratedSecret(secret, hash(secret), secret.substring(0, DISPLAY_PREFIX_LENGTH));
    }

    @Override
    public String hash(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}
