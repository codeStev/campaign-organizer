package com.campaignorganizer.campaign.adapter.arc.in.web;

import com.campaignorganizer.campaign.adapter.arc.in.web.BeatWebDtos.BeatRequest;
import com.campaignorganizer.campaign.adapter.arc.in.web.BeatWebDtos.BeatResponse;
import com.campaignorganizer.campaign.application.arc.port.in.BeatCommands.CreateBeatCommand;
import com.campaignorganizer.campaign.application.arc.port.in.BeatCommands.UpdateBeatCommand;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BeatWebMapper {

    BeatResponse toResponse(ArcBeatView view);

    default CreateBeatCommand toCreateCommand(UUID worldId, UUID campaignId, UUID arcId,
                                              BeatRequest request) {
        return new CreateBeatCommand(worldId, campaignId, arcId, request.title(), request.body(),
                request.doneOrDefault(), request.articleIdsOrEmpty(), request.statblockIdsOrEmpty(),
                request.tableIdsOrEmpty(), request.deckIdsOrEmpty(), request.sessionId(),
                request.position());
    }

    default UpdateBeatCommand toUpdateCommand(UUID worldId, UUID campaignId, UUID arcId, UUID beatId,
                                              BeatRequest request) {
        return new UpdateBeatCommand(worldId, campaignId, arcId, beatId, request.title(), request.body(),
                request.doneOrDefault(), request.articleIdsOrEmpty(), request.statblockIdsOrEmpty(),
                request.tableIdsOrEmpty(), request.deckIdsOrEmpty(), request.sessionId(),
                request.position());
    }
}
