package com.campaignorganizer.campaign.application.campaign.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerView;
import com.campaignorganizer.campaign.domain.campaign.CampaignPlayer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CampaignPlayerViewMapper {

    CampaignPlayerView toView(CampaignPlayer entry);
}
