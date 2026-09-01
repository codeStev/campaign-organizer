package com.campaignorganizer.campaign.domain.campaign;

import java.time.Instant;
import java.util.UUID;

/**
 * One player's membership on a campaign's roster (aggregate root): which
 * player, and whether they're a guest. The whole roster for a campaign is
 * replaced as a set (ADR-0091) — this row has no update method, only
 * create/reconstitute, since edits are delete-then-recreate.
 */
public final class CampaignPlayer {

    private final UUID id;
    private final UUID campaignId;
    private final UUID playerId;
    private final boolean guest;
    private final Instant createdAt;

    private CampaignPlayer(UUID id, UUID campaignId, UUID playerId, boolean guest, Instant createdAt) {
        this.id = id;
        this.campaignId = campaignId;
        this.playerId = playerId;
        this.guest = guest;
        this.createdAt = createdAt;
    }

    public static CampaignPlayer create(UUID id, UUID campaignId, UUID playerId, boolean guest,
                                        Instant now) {
        return new CampaignPlayer(id, campaignId, playerId, guest, now);
    }

    public static CampaignPlayer reconstitute(UUID id, UUID campaignId, UUID playerId, boolean guest,
                                              Instant createdAt) {
        return new CampaignPlayer(id, campaignId, playerId, guest, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isGuest() {
        return guest;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
