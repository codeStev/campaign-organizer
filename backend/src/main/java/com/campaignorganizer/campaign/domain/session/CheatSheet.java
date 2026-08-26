package com.campaignorganizer.campaign.domain.session;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The per-session cheat sheet (aggregate root, FR-37): a drag-ordered list of
 * condensed fragments printed on one page. One sheet per session; loaded and
 * saved as a whole.
 */
public final class CheatSheet {

    private final UUID id;
    private final UUID sessionId;
    private List<CheatSheetFragment> fragments;
    private final Instant createdAt;
    private Instant updatedAt;

    private CheatSheet(UUID id, UUID sessionId, List<CheatSheetFragment> fragments,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(fragments);
    }

    public static CheatSheet create(UUID id, UUID sessionId, List<CheatSheetFragment> fragments,
                                    Instant now) {
        return new CheatSheet(id, sessionId, fragments, now, now);
    }

    public static CheatSheet reconstitute(UUID id, UUID sessionId,
                                          List<CheatSheetFragment> fragments,
                                          Instant createdAt, Instant updatedAt) {
        return new CheatSheet(id, sessionId, fragments, createdAt, updatedAt);
    }

    public void update(List<CheatSheetFragment> fragments, Instant now) {
        apply(fragments);
        this.updatedAt = now;
    }

    private void apply(List<CheatSheetFragment> fragments) {
        this.fragments = fragments == null ? List.of() : List.copyOf(fragments);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public List<CheatSheetFragment> getFragments() {
        return fragments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
