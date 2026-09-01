package com.campaignorganizer.campaign.adapter.campaign.in.web;

import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignRosterWebDtos.RosterEntryRequest;
import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignRosterWebDtos.RosterEntryResponse;
import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignRosterWebDtos.RosterRequest;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterCommands;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterCommands.RosterEntryInput;
import com.campaignorganizer.campaign.application.campaign.port.in.RosterEntry;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CampaignRosterWebMapper {

    RosterEntryResponse toResponse(RosterEntry entry);

    List<RosterEntryResponse> toResponses(List<RosterEntry> entries);

    RosterEntryInput toInput(RosterEntryRequest request);

    List<RosterEntryInput> toInputs(List<RosterEntryRequest> requests);

    default RosterCommands.SetCampaignRosterCommand toSetCommand(UUID worldId, UUID campaignId,
                                                                  RosterRequest request) {
        return new RosterCommands.SetCampaignRosterCommand(worldId, campaignId,
                toInputs(request.entries()));
    }
}
