package com.campaignorganizer.accounts.adapter.account.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model for an in-progress WebAuthn ceremony's server-held state (maps {@code webauthn_challenges}). */
@Entity
@Table(name = "webauthn_challenges")
public class WebAuthnChallengeJpaEntity {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "purpose", nullable = false, length = 20)
    private String purpose;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected WebAuthnChallengeJpaEntity() {
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
