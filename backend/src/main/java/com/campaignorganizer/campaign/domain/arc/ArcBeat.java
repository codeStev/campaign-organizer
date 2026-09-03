package com.campaignorganizer.campaign.domain.arc;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A story beat within an arc, linking articles/statblocks/tables/decks and optionally a session (aggregate root). */
public final class ArcBeat {

    private final UUID id;
    private final UUID arcId;
    private List<UUID> articleIds;
    private List<UUID> statblockIds;
    private List<UUID> encounterIds;
    private List<UUID> tableIds;
    private List<UUID> deckIds;
    private UUID sessionId;
    private UUID kindId;
    private String title;
    private String body;
    private boolean done;
    private int position;
    private final Instant createdAt;
    private Instant updatedAt;

    private ArcBeat(UUID id, UUID arcId, List<UUID> articleIds, List<UUID> statblockIds,
                    List<UUID> encounterIds, List<UUID> tableIds, List<UUID> deckIds, UUID sessionId,
                    UUID kindId, String title, String body, boolean done, int position, Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.arcId = arcId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(articleIds, statblockIds, encounterIds, tableIds, deckIds, sessionId, kindId, title, body,
                done, position);
    }

    public static ArcBeat create(UUID id, UUID arcId, List<UUID> articleIds, List<UUID> statblockIds,
                                 List<UUID> encounterIds, List<UUID> tableIds, List<UUID> deckIds,
                                 UUID sessionId, UUID kindId, String title, String body, boolean done,
                                 int position, Instant now) {
        return new ArcBeat(id, arcId, articleIds, statblockIds, encounterIds, tableIds, deckIds, sessionId,
                kindId, title, body, done, position, now, now);
    }

    public static ArcBeat reconstitute(UUID id, UUID arcId, List<UUID> articleIds, List<UUID> statblockIds,
                                       List<UUID> encounterIds, List<UUID> tableIds, List<UUID> deckIds,
                                       UUID sessionId, UUID kindId, String title, String body, boolean done,
                                       int position, Instant createdAt, Instant updatedAt) {
        return new ArcBeat(id, arcId, articleIds, statblockIds, encounterIds, tableIds, deckIds, sessionId,
                kindId, title, body, done, position, createdAt, updatedAt);
    }

    public void update(List<UUID> articleIds, List<UUID> statblockIds, List<UUID> encounterIds,
                       List<UUID> tableIds, List<UUID> deckIds, UUID sessionId, UUID kindId, String title,
                       String body, boolean done, int position, Instant now) {
        apply(articleIds, statblockIds, encounterIds, tableIds, deckIds, sessionId, kindId, title, body,
                done, position);
        this.updatedAt = now;
    }

    private void apply(List<UUID> articleIds, List<UUID> statblockIds, List<UUID> encounterIds,
                       List<UUID> tableIds, List<UUID> deckIds, UUID sessionId, UUID kindId, String title,
                       String body, boolean done, int position) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Beat title must not be blank");
        }
        this.articleIds = articleIds == null ? new ArrayList<>() : new ArrayList<>(articleIds);
        this.statblockIds = statblockIds == null ? new ArrayList<>() : new ArrayList<>(statblockIds);
        this.encounterIds = encounterIds == null ? new ArrayList<>() : new ArrayList<>(encounterIds);
        this.tableIds = tableIds == null ? new ArrayList<>() : new ArrayList<>(tableIds);
        this.deckIds = deckIds == null ? new ArrayList<>() : new ArrayList<>(deckIds);
        this.sessionId = sessionId;
        this.kindId = kindId;
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

    /** Structured, quantified encounters linked to this beat (ADR-0097). */
    public List<UUID> getEncounterIds() {
        return encounterIds;
    }

    /** Roll tables referenced for quick reference/printing in the session packet (FR-40). */
    public List<UUID> getTableIds() {
        return tableIds;
    }

    /** Card decks referenced for quick reference/printing in the session packet (FR-40). */
    public List<UUID> getDeckIds() {
        return deckIds;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    /** Optional GM-defined beat kind tag (ADR-0101), informational only. */
    public UUID getKindId() {
        return kindId;
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
