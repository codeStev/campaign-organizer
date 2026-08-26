package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.CheatSheetWebDtos.CheatSheetRequest;
import com.campaignorganizer.campaign.adapter.session.in.web.CheatSheetWebDtos.CheatSheetResponse;
import com.campaignorganizer.campaign.adapter.session.in.web.CheatSheetWebDtos.FragmentRequest;
import com.campaignorganizer.campaign.adapter.session.in.web.CheatSheetWebDtos.FragmentResponse;
import com.campaignorganizer.campaign.application.session.port.in.CheatSheetCommands;
import com.campaignorganizer.campaign.application.session.port.in.CheatSheetCommands.FragmentInput;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps cheat-sheet web DTOs ↔ commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface CheatSheetWebMapper {

    CheatSheetResponse toResponse(CheatSheetView view);

    FragmentResponse toFragmentResponse(CheatSheetView.FragmentView view);

    FragmentInput toInput(FragmentRequest request);

    List<FragmentInput> toInputs(List<FragmentRequest> requests);

    default CheatSheetCommands.PutCheatSheetCommand toPutCommand(UUID worldId, UUID campaignId,
                                                                 UUID sessionId,
                                                                 CheatSheetRequest request) {
        return new CheatSheetCommands.PutCheatSheetCommand(worldId, campaignId, sessionId,
                toInputs(request.fragments()));
    }
}
