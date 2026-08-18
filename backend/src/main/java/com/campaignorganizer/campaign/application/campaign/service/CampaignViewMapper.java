package com.campaignorganizer.campaign.application.campaign.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.domain.campaign.Campaign;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CampaignViewMapper {

    CampaignView toView(Campaign campaign);
}
