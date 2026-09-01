package com.campaignorganizer.campaign.adapter.encounter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

/** Embeddable row of the {@code encounter_entries} table (real FK to statblocks, unlike Clock.segments' JSONB). */
@Embeddable
public class EncounterEntryEmbeddable {

    @Column(name = "statblock_id", nullable = false)
    private UUID statblockId;

    @Column(nullable = false)
    private int quantity;

    protected EncounterEntryEmbeddable() {
    }

    public EncounterEntryEmbeddable(UUID statblockId, int quantity) {
        this.statblockId = statblockId;
        this.quantity = quantity;
    }

    public UUID getStatblockId() {
        return statblockId;
    }

    public void setStatblockId(UUID statblockId) {
        this.statblockId = statblockId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
