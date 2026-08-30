package com.campaignorganizer.handouts.domain;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A player-facing handout (aggregate root, FR-46): one page of in-world
 * prose — a letter, wanted poster, newspaper piece — printed with a fixed
 * visual style. Deliberately separate from GM-only article content: handouts
 * are props that go to the players.
 */
public final class Handout {

    /** Fixed print-style presets; the frontend ships a matching stylesheet. */
    public enum Preset {
        PARCHMENT, NEWSPAPER, POSTER, LETTER
    }

    private final UUID id;
    private final UUID worldId;
    private String title;
    private Preset preset;
    private String body;
    /** Optional session this handout is prepped for (ADR-0077); null means unassigned. */
    private UUID sessionId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Handout(UUID id, UUID worldId, String title, Preset preset, String body,
                    UUID sessionId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(title, preset, body);
    }

    public static Handout create(UUID id, UUID worldId, String title, Preset preset,
                                 String body, UUID sessionId, Instant now) {
        return new Handout(id, worldId, title, preset, body, sessionId, now, now);
    }

    public static Handout reconstitute(UUID id, UUID worldId, String title, Preset preset,
                                       String body, UUID sessionId, Instant createdAt,
                                       Instant updatedAt) {
        return new Handout(id, worldId, title, preset, body, sessionId, createdAt, updatedAt);
    }

    public void update(String title, Preset preset, String body, UUID sessionId, Instant now) {
        apply(title, preset, body);
        this.sessionId = sessionId;
        this.updatedAt = now;
    }

    private void apply(String title, Preset preset, String body) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Handout title must not be blank");
        }
        if (title.length() > 200) {
            throw new ValidationException("Handout title must not exceed 200 characters");
        }
        if (preset == null) {
            throw new ValidationException("Handout style must not be blank");
        }
        this.title = title;
        this.preset = preset;
        this.body = body == null ? "" : body;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getTitle() {
        return title;
    }

    public Preset getPreset() {
        return preset;
    }

    public String getBody() {
        return body;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
