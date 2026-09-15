package com.campaignorganizer.accounts.adapter.account.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model for a single-use OIDC login redirect handoff (maps {@code oidc_login_exchanges}). */
@Entity
@Table(name = "oidc_login_exchanges")
public class OidcLoginExchangeJpaEntity {

    @Id
    private UUID code;

    @Column(name = "login_response_json", nullable = false, columnDefinition = "TEXT")
    private String loginResponseJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected OidcLoginExchangeJpaEntity() {
    }

    public UUID getCode() {
        return code;
    }

    public void setCode(UUID code) {
        this.code = code;
    }

    public String getLoginResponseJson() {
        return loginResponseJson;
    }

    public void setLoginResponseJson(String loginResponseJson) {
        this.loginResponseJson = loginResponseJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
