package com.campaignorganizer.campaign.application.campaign.port.published;

/** Published import for backup/restore (ADR-0061): saves a roster row verbatim. */
public interface CampaignPlayerImportPort {

    CampaignPlayerView importCampaignPlayer(CampaignPlayerView view);
}
