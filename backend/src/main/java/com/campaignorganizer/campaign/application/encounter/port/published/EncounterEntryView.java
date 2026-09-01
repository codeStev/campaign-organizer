package com.campaignorganizer.campaign.application.encounter.port.published;

import java.util.UUID;

/** Published read model for one encounter entry. */
public record EncounterEntryView(UUID statblockId, int quantity) {
}
