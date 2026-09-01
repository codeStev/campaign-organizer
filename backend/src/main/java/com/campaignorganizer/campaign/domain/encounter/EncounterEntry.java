package com.campaignorganizer.campaign.domain.encounter;

import java.util.UUID;

/** One statblock's slot in an encounter, with how many copies (value object, no own id, ADR-0097). */
public record EncounterEntry(UUID statblockId, int quantity, Integer maxHpOverride) {
}
