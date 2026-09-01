package com.campaignorganizer.campaign.domain.session;

import java.time.Instant;
import java.util.UUID;

/**
 * One player's attendance row for one session (aggregate root, ADR-0091):
 * whether they were present, and which character sheet (if any) they played
 * that session. The whole set is replaced per session — this row has no
 * update method, only create/reconstitute, since edits are delete-then-
 * recreate. {@code playerId} references the global {@link
 * com.campaignorganizer.campaign.domain.player.Player} directly (not the
 * campaign roster membership), so attendance history survives a later
 * roster change.
 */
public final class SessionAttendance {

    private final UUID id;
    private final UUID sessionId;
    private final UUID playerId;
    private final boolean present;
    private final UUID characterId;
    private final Instant createdAt;

    private SessionAttendance(UUID id, UUID sessionId, UUID playerId, boolean present,
                              UUID characterId, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.present = present;
        this.characterId = characterId;
        this.createdAt = createdAt;
    }

    public static SessionAttendance create(UUID id, UUID sessionId, UUID playerId, boolean present,
                                           UUID characterId, Instant now) {
        return new SessionAttendance(id, sessionId, playerId, present, characterId, now);
    }

    public static SessionAttendance reconstitute(UUID id, UUID sessionId, UUID playerId,
                                                 boolean present, UUID characterId,
                                                 Instant createdAt) {
        return new SessionAttendance(id, sessionId, playerId, present, characterId, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isPresent() {
        return present;
    }

    public UUID getCharacterId() {
        return characterId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
