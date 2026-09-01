package com.campaignorganizer.campaign.application.encounter.port.in;

import java.util.UUID;

public interface DeleteEncounterUseCase {

    void delete(UUID worldId, UUID campaignId, UUID encounterId);
}
