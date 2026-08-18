package com.campaignorganizer.campaign.domain.arc;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A story beat within an arc, linking articles/statblocks and optionally a session (aggregate root). */
public final class ArcBeat {

    private final UUID id;
    private final UUID arcId;
    private List<UUID> articleIds;
    private List<UUID> statblockIds;
    private UUID sessionId;
    private String title;
    private String body;
    private boolean done;
    private int position;
    private final Instant createdAt;
    private Instant updatedAt;

    private ArcBeat(UUID id, UUID arcId, List<UUID> articleIds, List<UUID> statblockIds, UUID sessionId,
                    String title, String body, boolean done, int position, Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.arcId = arcId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(articleIds, statblockIds, sessionId, title, body, done, position);
    }

    public static ArcBeat create(UUID id, UUID arcId, List<UUID> articleIds, List<UUID> statblockIds,
                                 UUID sessionId, String title, String body, boolean done, int position,
                                 Instant now) {
        return new ArcBeat(id, arcId, articleIds, statblockIds, sessionId, title, body, done, position,
                now, now);
    }

    public static ArcBeat reconstitute(UUID id, UUID arcId, List<UUID> articleIds, List<UUID> statblockIds,
                                       UUID sessionId, String title, String body, boolean done,
                                       int position, Instant createdAt, Instant updatedAt) {
        return new ArcBeat(id, arcId, articleIds, statblockIds, sessionId, title, body, done, position,
                createdAt, updatedAt);
    }

    public void update(List<UUID> articleIds, List<UUID> statblockIds, UUID sessionId, String title,
                       String body, boolean done, int position, Instant now) {
        apply(articleIds, statblockIds, sessionId, title, body, done, position);
        this.updatedAt = now;
    }

    private void apply(List<UUID> articleIds, List<UUID> statblockIds, UUID sessionId, String title,
                       String body, boolean done, int position) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Beat title must not be blank");
        }
        this.articleIds = articleIds == null ? new ArrayList<>() : new ArrayList<>(articleIds);
        this.statblockIds = statblockIds == null ? new ArrayList<>() : new ArrayList<>(statblockIds);
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

    public List<UUID> getStatblockIds() {
        return statblockIds;
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
