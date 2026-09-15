package com.campaignorganizer.accounts.domain.account;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * An account: email + hashed password, a role, and enough lifecycle state
 * (enabled flag, token version, failed-login tracking) to support immediate
 * JWT revocation and basic brute-force throttling (ADR-0109/ADR-0110).
 * Never carries the raw password — only its hash, set by the application
 * layer (which owns the {@code PasswordEncoder}, a Spring type this
 * framework-free domain type must not depend on).
 */
public final class Account {

    private final UUID id;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean enabled;
    private int tokenVersion;
    private int failedAttempts;
    private Instant lockedUntil;
    private MfaMethod mfaMethod;
    private String totpSecretEncrypted;
    private String totpSecretPendingEncrypted;
    private final Instant createdAt;
    private Instant updatedAt;

    private Account(UUID id, String email, String passwordHash, Role role, boolean enabled, int tokenVersion,
                    int failedAttempts, Instant lockedUntil, MfaMethod mfaMethod, String totpSecretEncrypted,
                    String totpSecretPendingEncrypted, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.mfaMethod = mfaMethod == null ? MfaMethod.NONE : mfaMethod;
        this.totpSecretEncrypted = totpSecretEncrypted;
        this.totpSecretPendingEncrypted = totpSecretPendingEncrypted;
        applyEmail(email);
        applyPasswordHash(passwordHash);
        this.role = requireRole(role);
    }

    public static Account create(UUID id, String email, String passwordHash, Role role, Instant now) {
        return new Account(id, email, passwordHash, role, true, 0, 0, null, MfaMethod.NONE, null, null, now, now);
    }

    public static Account reconstitute(UUID id, String email, String passwordHash, Role role, boolean enabled,
                                       int tokenVersion, int failedAttempts, Instant lockedUntil, MfaMethod mfaMethod,
                                       String totpSecretEncrypted, String totpSecretPendingEncrypted,
                                       Instant createdAt, Instant updatedAt) {
        return new Account(id, email, passwordHash, role, enabled, tokenVersion, failedAttempts, lockedUntil,
                mfaMethod, totpSecretEncrypted, totpSecretPendingEncrypted, createdAt, updatedAt);
    }

    /** Password change bumps the token version: every previously-issued token stops working. */
    public void changePasswordHash(String newPasswordHash, Instant now) {
        applyPasswordHash(newPasswordHash);
        this.tokenVersion++;
        this.updatedAt = now;
    }

    /** Role change bumps the token version: a demoted admin's existing token loses elevated rights immediately. */
    public void changeRole(Role role, Instant now) {
        this.role = requireRole(role);
        this.tokenVersion++;
        this.updatedAt = now;
    }

    /** Disabling bumps the token version: an already-issued token stops working immediately, not just new logins. */
    public void disable(Instant now) {
        this.enabled = false;
        this.tokenVersion++;
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        this.enabled = true;
        this.updatedAt = now;
    }

    /** Self- or admin-triggered "log out everywhere": bumps the token version with no other side effect. */
    public void logoutAll(Instant now) {
        this.tokenVersion++;
        this.updatedAt = now;
    }

    /**
     * Stores a new in-progress TOTP secret pending confirmation. Doesn't touch the active
     * method/secret — calling this again before confirming just replaces the pending attempt.
     * Only valid while no MFA method is active yet — first-time enrollment. Self-service
     * *replacement* of an already-active TOTP secret goes through {@link
     * #beginTotpReEnrollment} instead (ADR-0111 follow-up); an admin-triggered full reset
     * still goes through {@link #resetMfaForRecovery}.
     */
    public void beginTotpEnrollment(String pendingSecretEncrypted, Instant now) {
        if (mfaMethod != MfaMethod.NONE) {
            throw new ValidationException("Account already has an active MFA method");
        }
        if (pendingSecretEncrypted == null || pendingSecretEncrypted.isBlank()) {
            throw new ValidationException("Pending TOTP secret must not be blank");
        }
        this.totpSecretPendingEncrypted = pendingSecretEncrypted;
        this.updatedAt = now;
    }

    /**
     * Stores a new in-progress TOTP secret pending confirmation, same as {@link
     * #beginTotpEnrollment} but for an account whose active method is *already* TOTP — e.g.
     * replacing a lost/retired authenticator without an admin reset (ADR-0111 follow-up).
     * Deliberately the opposite precondition from {@link #beginTotpEnrollment}: this method
     * exists so the two can be gated by different routes/authorization requirements at the
     * application layer (first enrollment needs only the PASSWORD factor; this needs the MFA
     * factor already proven) without either accidentally accepting the other's starting state.
     */
    public void beginTotpReEnrollment(String pendingSecretEncrypted, Instant now) {
        if (mfaMethod != MfaMethod.TOTP) {
            throw new ValidationException("TOTP is not this account's active MFA method");
        }
        if (pendingSecretEncrypted == null || pendingSecretEncrypted.isBlank()) {
            throw new ValidationException("Pending TOTP secret must not be blank");
        }
        this.totpSecretPendingEncrypted = pendingSecretEncrypted;
        this.updatedAt = now;
    }

    /**
     * Promotes the pending TOTP secret to active once its code has been verified by the caller.
     * Bumps the token version — found missing during this feature's own {@code security-review}
     * pass for the re-enrollment case specifically: without it, replacing a lost/stolen device's
     * TOTP secret (the whole point of {@link #beginTotpReEnrollment}) left that device's
     * already-issued token fully valid until its natural expiry, exactly the scenario a user
     * reaching for this flow is trying to shut out. Harmless for first-time enrollment too — the
     * caller there already gets a brand-new token immediately after, the same as every other
     * token-invalidating mutator on this aggregate ({@link #changePasswordHash}, {@link
     * #disable}, {@link #resetMfaForRecovery}).
     */
    public void completeTotpEnrollment(Instant now) {
        if (totpSecretPendingEncrypted == null) {
            throw new ValidationException("No pending TOTP enrollment to confirm");
        }
        this.mfaMethod = MfaMethod.TOTP;
        this.totpSecretEncrypted = totpSecretPendingEncrypted;
        this.totpSecretPendingEncrypted = null;
        this.tokenVersion++;
        this.updatedAt = now;
    }

    /**
     * Activates WebAuthn as the account's MFA method once a credential has been successfully
     * registered. Unlike TOTP there's no "pending secret" to promote here — the credential
     * itself lives in the separate webauthn_credentials table, and the ceremony's challenge
     * state lives in its own stateless repository, neither on this aggregate.
     */
    public void completeWebauthnEnrollment(Instant now) {
        if (mfaMethod != MfaMethod.NONE) {
            throw new ValidationException("Account already has an active MFA method");
        }
        this.mfaMethod = MfaMethod.WEBAUTHN;
        this.updatedAt = now;
    }

    /**
     * Spending a recovery code to regain access resets MFA back to unset rather than granting
     * full access outright — the lost device may be gone for good, so re-enrollment is forced
     * through the normal setup path. Bumps the token version: any other outstanding token
     * (a stale session on another device) is invalidated by this same security-relevant event.
     */
    public void resetMfaForRecovery(Instant now) {
        this.mfaMethod = MfaMethod.NONE;
        this.totpSecretEncrypted = null;
        this.totpSecretPendingEncrypted = null;
        this.tokenVersion++;
        this.updatedAt = now;
    }

    public void recordFailedLogin(Instant now, int maxAttempts, Duration lockoutDuration) {
        this.failedAttempts++;
        if (this.failedAttempts >= maxAttempts) {
            this.lockedUntil = now.plus(lockoutDuration);
        }
        this.updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    private void applyEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Account email must not be blank");
        }
        this.email = email;
    }

    private void applyPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new ValidationException("Account password hash must not be blank");
        }
        this.passwordHash = passwordHash;
    }

    private static Role requireRole(Role role) {
        if (role == null) {
            throw new ValidationException("Account role must not be null");
        }
        return role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public MfaMethod getMfaMethod() {
        return mfaMethod;
    }

    public String getTotpSecretEncrypted() {
        return totpSecretEncrypted;
    }

    public String getTotpSecretPendingEncrypted() {
        return totpSecretPendingEncrypted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
