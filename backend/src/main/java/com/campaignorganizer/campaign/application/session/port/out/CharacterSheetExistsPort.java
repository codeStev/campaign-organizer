package com.campaignorganizer.campaign.application.session.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * ACL out-port to the {@code characters} context (ADR-0091): a session
 * attendance row's linked character must exist in the world and be either
 * shared (no campaign) or scoped to this session's own campaign.
 */
public interface CharacterSheetExistsPort {

    boolean existsForCampaign(UUID characterId, UUID worldId, UUID campaignId);

    Optional<String> findName(UUID characterId, UUID worldId);
}
