package com.campaignorganizer.accounts.adapter.account.out.mfa;

import com.campaignorganizer.accounts.application.account.port.out.TotpPort;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Wraps {@code dev.samstevens.totp} behind {@link TotpPort}. SHA1/6-digits/30s (the
 * {@code HashingAlgorithm.SHA1} default below) isn't the strongest option the library
 * supports, but it's the one every mainstream authenticator app (Google Authenticator, Authy,
 * 1Password, ...) actually implements — SHA256/SHA512 secrets would silently fail to scan or
 * verify in most of them, and SHA1's known weaknesses (collision attacks) aren't relevant to
 * HMAC-SHA1's use here.
 */
@Component
public class TotpAdapter implements TotpPort {

    private static final String ISSUER = "Campaign Organizer";
    /** ±1 time step (30s) of clock drift tolerance either side of the server's own clock. */
    private static final int ALLOWED_TIME_PERIOD_DISCREPANCY = 1;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrDataFactory qrDataFactory = new QrDataFactory(HashingAlgorithm.SHA1, 6, 30);
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier;

    public TotpAdapter() {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
        verifier.setAllowedTimePeriodDiscrepancy(ALLOWED_TIME_PERIOD_DISCREPANCY);
        this.codeVerifier = verifier;
    }

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public String provisioningUri(String secret, String accountEmail) {
        return buildQrData(secret, accountEmail).getUri();
    }

    @Override
    public String qrCodeDataUri(String secret, String accountEmail) {
        try {
            byte[] png = qrGenerator.generate(buildQrData(secret, accountEmail));
            return "data:" + qrGenerator.getImageMimeType() + ";base64," + Base64.getEncoder().encodeToString(png);
        } catch (QrGenerationException ex) {
            throw new IllegalStateException("Failed to render TOTP QR code", ex);
        }
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    private QrData buildQrData(String secret, String accountEmail) {
        return qrDataFactory.newBuilder()
                .label(accountEmail)
                .secret(secret)
                .issuer(ISSUER)
                .build();
    }
}
