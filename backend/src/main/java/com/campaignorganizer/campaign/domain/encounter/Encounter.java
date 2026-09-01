package com.campaignorganizer.campaign.domain.encounter;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A named, reusable, printable grouping of statblocks scoped to a campaign
 * (aggregate root, ADR-0097) - any number of statblocks with quantities,
 * optionally linked to one or more arc beats. Distinct from
 * {@code ArcBeat.statblockIds} (ADR-0043), which stays for loose statblock
 * references that aren't a structured combat grouping.
 */
public final class Encounter {

    private final UUID id;
    private final UUID campaignId;
    private String name;
    private String notes;
    private List<EncounterEntry> entries;
    private final Instant createdAt;
    private Instant updatedAt;

    private Encounter(UUID id, UUID campaignId, String name, String notes, List<EncounterEntry> entries,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.campaignId = campaignId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, notes, entries);
    }

    public static Encounter create(UUID id, UUID campaignId, String name, String notes,
                                   List<EncounterEntry> entries, Instant now) {
        return new Encounter(id, campaignId, name, notes, entries, now, now);
    }

    public static Encounter reconstitute(UUID id, UUID campaignId, String name, String notes,
                                         List<EncounterEntry> entries, Instant createdAt, Instant updatedAt) {
        return new Encounter(id, campaignId, name, notes, entries, createdAt, updatedAt);
    }

    public void update(String name, String notes, List<EncounterEntry> entries, Instant now) {
        apply(name, notes, entries);
        this.updatedAt = now;
    }

    private void apply(String name, String notes, List<EncounterEntry> entries) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Encounter name must not be blank");
        }
        List<EncounterEntry> normalized = entries == null ? List.of() : List.copyOf(entries);
        for (EncounterEntry entry : normalized) {
            if (entry.statblockId() == null) {
                throw new ValidationException("Encounter entry must reference a statblock");
            }
            if (entry.quantity() < 1) {
                throw new ValidationException("Encounter entry quantity must be at least 1");
            }
        }
        this.name = name;
        this.notes = notes;
        this.entries = normalized;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public List<EncounterEntry> getEntries() {
        return entries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
