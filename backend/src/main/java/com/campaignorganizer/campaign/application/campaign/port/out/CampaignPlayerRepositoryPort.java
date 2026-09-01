package com.campaignorganizer.campaign.application.campaign.port.out;

import com.campaignorganizer.campaign.domain.campaign.CampaignPlayer;
import java.util.List;
import java.util.UUID;

public interface CampaignPlayerRepositoryPort {

    List<CampaignPlayer> findByCampaign(UUID campaignId);

    CampaignPlayer save(CampaignPlayer entry);

    /** Deletes every existing roster row for this campaign, for a whole-set replace. */
    void deleteByCampaign(UUID campaignId);
}
