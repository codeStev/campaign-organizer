package com.campaignorganizer.campaign.adapter.session.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persistence model for a session cheat sheet (dumb carrier — no logic, no
 * id/time generation; those come from the domain via the mapper). Maps the
 * {@code cheat_sheets} table; fragments are one JSONB payload.
 */
@Entity
@Table(name = "cheat_sheets")
public class CheatSheetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true, updatable = false)
    private UUID sessionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<CheatSheetFragmentJson> fragments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CheatSheetJpaEntity() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public List<CheatSheetFragmentJson> getFragments() {
        return fragments;
    }

    public void setFragments(List<CheatSheetFragmentJson> fragments) {
        this.fragments = fragments == null ? new ArrayList<>() : fragments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
