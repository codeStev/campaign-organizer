package com.campaignorganizer.accounts.adapter.account.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persistence model for an in-progress OIDC login's authorization request (maps {@code oidc_authorization_requests}). */
@Entity
@Table(name = "oidc_authorization_requests")
public class OidcAuthorizationRequestJpaEntity {

    @Id
    @Column(name = "state", length = 200)
    private String state;

    @Column(name = "request_json", nullable = false, columnDefinition = "TEXT")
    private String requestJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected OidcAuthorizationRequestJpaEntity() {
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public void setRequestJson(String requestJson) {
        this.requestJson = requestJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
