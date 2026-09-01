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

    @Column(name = "max_hp_override")
    private Integer maxHpOverride;

    protected EncounterEntryEmbeddable() {
    }

    public EncounterEntryEmbeddable(UUID statblockId, int quantity, Integer maxHpOverride) {
        this.statblockId = statblockId;
        this.quantity = quantity;
        this.maxHpOverride = maxHpOverride;
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

    public Integer getMaxHpOverride() {
        return maxHpOverride;
    }

    public void setMaxHpOverride(Integer maxHpOverride) {
        this.maxHpOverride = maxHpOverride;
    }
}
