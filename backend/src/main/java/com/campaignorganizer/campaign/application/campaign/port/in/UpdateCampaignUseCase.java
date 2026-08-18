package com.campaignorganizer.campaign.application.campaign.port.in;

import com.campaignorganizer.campaign.application.campaign.port.in.CampaignCommands.UpdateCampaignCommand;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;

public interface UpdateCampaignUseCase {

    CampaignView update(UpdateCampaignCommand command);
}
