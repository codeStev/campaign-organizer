package com.campaignorganizer.campaign.application.campaign.port.published;

/**
 * Published port: persists a campaign exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id — backup
 * import's counterpart to the normal create flow (ADR-0061).
 */
public interface CampaignImportPort {

    CampaignView importCampaign(CampaignView view);
}
