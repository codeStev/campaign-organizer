package com.campaignorganizer.campaign.application.encounter.port.in;

import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;
import java.util.List;
import java.util.UUID;

public interface ListEncountersUseCase {

    List<EncounterView> list(UUID worldId, UUID campaignId);
}
