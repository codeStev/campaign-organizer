package com.campaignorganizer.ai.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Persistence model for one provider's settings (maps the {@code ai_provider_settings} table). */
@Entity
@Table(name = "ai_provider_settings")
public class AiProviderSettingsJpaEntity {

    @Id
    @Column(name = "provider", length = 50)
    private String providerId;

    @Column(length = 200)
    private String model;

    @Column(nullable = false)
    private int priority;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiProviderSettingsJpaEntity() {
        // for JPA
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
