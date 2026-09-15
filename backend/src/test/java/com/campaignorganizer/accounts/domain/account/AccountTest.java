package com.campaignorganizer.accounts.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the account aggregate's MFA lifecycle (ADR-0111). */
class AccountTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void freshAccountHasNoMfaMethod() {
        Account account = create();

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(account.getTotpSecretEncrypted()).isNull();
        assertThat(account.getTotpSecretPendingEncrypted()).isNull();
    }

    @Test
    void beginTotpEnrollmentStoresPendingSecretOnly() {
        Account account = create();

        account.beginTotpEnrollment("encrypted-secret", T1);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(account.getTotpSecretPendingEncrypted()).isEqualTo("encrypted-secret");
        assertThat(account.getTotpSecretEncrypted()).isNull();
        assertThat(account.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void completeTotpEnrollmentPromotesPendingSecretToActive() {
        Account account = create();
        account.beginTotpEnrollment("encrypted-secret", T0);

        account.completeTotpEnrollment(T1);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(account.getTotpSecretEncrypted()).isEqualTo("encrypted-secret");
        assertThat(account.getTotpSecretPendingEncrypted()).isNull();
    }

    @Test
    void completeTotpEnrollmentFailsWithoutAPendingSecret() {
        Account account = create();

        assertThatThrownBy(() -> account.completeTotpEnrollment(T1)).isInstanceOf(ValidationException.class);
    }

    @Test
    void beginTotpEnrollmentFailsWhenAMethodIsAlreadyActive() {
        Account account = create();
        account.beginTotpEnrollment("encrypted-secret", T0);
        account.completeTotpEnrollment(T0);

        assertThatThrownBy(() -> account.beginTotpEnrollment("another-secret", T1))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void completeWebauthnEnrollmentActivatesWebauthnAsMfaMethod() {
        Account account = create();

        account.completeWebauthnEnrollment(T1);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.WEBAUTHN);
        assertThat(account.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void completeWebauthnEnrollmentFailsWhenAMethodIsAlreadyActive() {
        Account account = create();
        account.beginTotpEnrollment("encrypted-secret", T0);
        account.completeTotpEnrollment(T0);

        assertThatThrownBy(() -> account.completeWebauthnEnrollment(T1)).isInstanceOf(ValidationException.class);
    }

    @Test
    void resetMfaForRecoveryClearsMethodAndSecretsAndBumpsTokenVersion() {
        Account account = create();
        account.beginTotpEnrollment("encrypted-secret", T0);
        account.completeTotpEnrollment(T0);
        int versionBefore = account.getTokenVersion();

        account.resetMfaForRecovery(T1);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.NONE);
        assertThat(account.getTotpSecretEncrypted()).isNull();
        assertThat(account.getTotpSecretPendingEncrypted()).isNull();
        assertThat(account.getTokenVersion()).isEqualTo(versionBefore + 1);
    }

    private static Account create() {
        return Account.create(UUID.randomUUID(), "gm@example.com", "hash", Role.USER, T0);
    }
}
