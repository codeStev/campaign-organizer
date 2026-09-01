package com.campaignorganizer.campaign.application.campaign.port.in;

import java.util.List;
import java.util.UUID;

public interface GetCampaignRosterUseCase {

    List<RosterEntry> get(UUID worldId, UUID campaignId);
}
