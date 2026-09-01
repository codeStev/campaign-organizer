package com.campaignorganizer.campaign.application.campaign.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read a campaign's roster from sibling aggregates (session attendance, export). */
public interface CampaignPlayerQueryPort {

    List<CampaignPlayerView> findByCampaign(UUID campaignId);
}
