package com.campaignorganizer.campaign.domain.encounter;

import java.util.UUID;

/**
 * One statblock's slot in an encounter, with how many copies (value object,
 * no own id, ADR-0097). Deliberately carries no HP/resource override -
 * not every system tracks HP (Forbidden Lands, Vaesen, ...); whatever a
 * combatant's trackable resource is stays live/auto-detected and editable
 * at print time (ADR-0069), same as the ad-hoc flow.
 */
public record EncounterEntry(UUID statblockId, int quantity) {
}
