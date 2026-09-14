package com.campaignorganizer.accounts.application.account.port.out;

/**
 * TOTP generation/verification (ADR-0111), kept out of {@code MfaService} the same way
 * {@code PasswordEncoder} is injected rather than called statically — swappable and
 * independently testable.
 */
public interface TotpPort {

    /** A fresh Base32 secret for a new enrollment attempt. */
    String generateSecret();

    /** The {@code otpauth://} URI an authenticator app can also accept via manual entry/deep link. */
    String provisioningUri(String secret, String accountEmail);

    /** {@code data:image/png;base64,...} — the same provisioning URI, rendered as a scannable QR code. */
    String qrCodeDataUri(String secret, String accountEmail);

    /** Whether {@code code} is currently valid for {@code secret}, allowing for small clock drift. */
    boolean verifyCode(String secret, String code);
}
