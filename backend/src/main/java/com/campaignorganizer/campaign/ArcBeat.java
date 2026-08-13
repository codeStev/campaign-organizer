package com.campaignorganizer.campaign;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "arc_beats")
public class ArcBeat {

    @Id
    private UUID id;

    @Column(name = "arc_id", nullable = false, updatable = false)
    private UUID arcId;

    /** Articles this beat concerns (place, NPC, item…); stored in beat_articles (ADR-0032). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "beat_articles", joinColumns = @JoinColumn(name = "beat_id"))
    @Column(name = "article_id")
    private List<UUID> articleIds = new ArrayList<>();

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ArcBeat() {
        // for JPA
    }

    public ArcBeat(UUID arcId, List<UUID> articleIds, UUID sessionId, String title, String body,
                   boolean done, int position) {
        this.arcId = arcId;
        this.articleIds = articleIds == null ? new ArrayList<>() : new ArrayList<>(articleIds);
        this.sessionId = sessionId;
        this.title = title;
        this.body = body;
        this.done = done;
        this.position = position;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(List<UUID> articleIds, UUID sessionId, String title, String body,
                       boolean done, int position) {
        this.articleIds = articleIds == null ? new ArrayList<>() : new ArrayList<>(articleIds);
        this.sessionId = sessionId;
        this.title = title;
        this.body = body;
        this.done = done;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getArcId() {
        return arcId;
    }

    public List<UUID> getArticleIds() {
        return articleIds;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isDone() {
        return done;
    }

    public int getPosition() {
        return position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
