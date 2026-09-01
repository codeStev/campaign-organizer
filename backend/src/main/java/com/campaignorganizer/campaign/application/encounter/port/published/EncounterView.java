package com.campaignorganizer.campaign.application.encounter.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model for an encounter. */
public record EncounterView(
        UUID id,
        UUID campaignId,
        String name,
        String notes,
        List<EncounterEntryView> entries,
        Instant createdAt,
        Instant updatedAt) {
}
