package com.campaignorganizer.campaign.adapter.campaign.in.web;

import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignWebDtos.CampaignRequest;
import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignWebDtos.CampaignResponse;
import com.campaignorganizer.campaign.application.campaign.port.in.CampaignCommands.CreateCampaignCommand;
import com.campaignorganizer.campaign.application.campaign.port.in.CampaignCommands.UpdateCampaignCommand;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CampaignWebMapper {

    CampaignResponse toResponse(CampaignView view);

    default CreateCampaignCommand toCreateCommand(UUID worldId, CampaignRequest request) {
        return new CreateCampaignCommand(worldId, request.name(), request.description(), request.notes(),
                request.status(), request.systemId());
    }

    default UpdateCampaignCommand toUpdateCommand(UUID worldId, UUID campaignId,
                                                  CampaignRequest request) {
        return new UpdateCampaignCommand(worldId, campaignId, request.name(), request.description(),
                request.notes(), request.status(), request.systemId());
    }
}
