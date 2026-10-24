package com.campaignorganizer.interchange.companion.application.port.in;

import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import java.util.List;
import java.util.UUID;

/** Every campaign of every world the account owns, for companion apps (ADR-0120). */
public interface ListCompanionCampaignsUseCase {

    List<CompanionCampaign> list(UUID accountId);

    record CompanionCampaign(UUID id, String name, CampaignStatus status, UUID worldId, String worldName) {
    }
}
