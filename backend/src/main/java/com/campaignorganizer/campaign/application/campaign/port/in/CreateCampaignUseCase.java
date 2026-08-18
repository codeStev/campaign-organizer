package com.campaignorganizer.campaign.application.campaign.port.in;

import com.campaignorganizer.campaign.application.campaign.port.in.CampaignCommands.CreateCampaignCommand;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;

public interface CreateCampaignUseCase {

    CampaignView create(CreateCampaignCommand command);
}
