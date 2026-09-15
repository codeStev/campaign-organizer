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
        int versionBefore = account.getTokenVersion();

        account.completeTotpEnrollment(T1);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(account.getTotpSecretEncrypted()).isEqualTo("encrypted-secret");
        assertThat(account.getTotpSecretPendingEncrypted()).isNull();
        assertThat(account.getTokenVersion()).isEqualTo(versionBefore + 1);
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
    void beginTotpReEnrollmentStoresPendingSecretWhileKeepingTotpActive() {
        Account account = create();
        account.beginTotpEnrollment("encrypted-secret", T0);
        account.completeTotpEnrollment(T0);

        account.beginTotpReEnrollment("replacement-secret", T1);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(account.getTotpSecretPendingEncrypted()).isEqualTo("replacement-secret");
        assertThat(account.getTotpSecretEncrypted()).isEqualTo("encrypted-secret");
        assertThat(account.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void completeTotpEnrollmentAfterReEnrollmentSwapsTheActiveSecretAndBumpsTokenVersion() {
        Account account = create();
        account.beginTotpEnrollment("encrypted-secret", T0);
        account.completeTotpEnrollment(T0);
        account.beginTotpReEnrollment("replacement-secret", T0);
        int versionBefore = account.getTokenVersion();

        account.completeTotpEnrollment(T1);

        assertThat(account.getMfaMethod()).isEqualTo(MfaMethod.TOTP);
        assertThat(account.getTotpSecretEncrypted()).isEqualTo("replacement-secret");
        assertThat(account.getTotpSecretPendingEncrypted()).isNull();
        // The whole point of re-enrollment is replacing a lost/stolen device's secret — its
        // already-issued token must stop working immediately, not just future logins.
        assertThat(account.getTokenVersion()).isEqualTo(versionBefore + 1);
    }

    @Test
    void beginTotpReEnrollmentFailsWhenNoMfaMethodIsActiveYet() {
        Account account = create();

        assertThatThrownBy(() -> account.beginTotpReEnrollment("secret", T1))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void beginTotpReEnrollmentFailsWhenTheActiveMethodIsWebauthnNotTotp() {
        Account account = create();
        account.completeWebauthnEnrollment(T0);

        assertThatThrownBy(() -> account.beginTotpReEnrollment("secret", T1))
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

    @Test
    void createdViaOidcHasNoPasswordAndCarriesTheExternalIdentity() {
        Account account = Account.createViaOidc(UUID.randomUUID(), "gm@example.com", "GOOGLE", "subject-123",
                Role.USER, T0);

        assertThat(account.getPasswordHash()).isNull();
        assertThat(account.getAuthProvider()).isEqualTo("GOOGLE");
        assertThat(account.getExternalSubject()).isEqualTo("subject-123");
    }

    @Test
    void reconstitutingWithBothAPasswordAndAnExternalIdentityFails() {
        assertThatThrownBy(() -> Account.reconstitute(UUID.randomUUID(), "gm@example.com", "hash", "GOOGLE",
                "subject-123", Role.USER, true, 0, 0, null, MfaMethod.NONE, null, null, T0, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reconstitutingWithNeitherAPasswordNorAnExternalIdentityFails() {
        assertThatThrownBy(() -> Account.reconstitute(UUID.randomUUID(), "gm@example.com", null, null, null,
                Role.USER, true, 0, 0, null, MfaMethod.NONE, null, null, T0, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reconstitutingWithOnlyAPartialExternalIdentityFails() {
        assertThatThrownBy(() -> Account.reconstitute(UUID.randomUUID(), "gm@example.com", null, "GOOGLE", null,
                Role.USER, true, 0, 0, null, MfaMethod.NONE, null, null, T0, T0))
                .isInstanceOf(ValidationException.class);
    }

    private static Account create() {
        return Account.create(UUID.randomUUID(), "gm@example.com", "hash", Role.USER, T0);
    }
}
