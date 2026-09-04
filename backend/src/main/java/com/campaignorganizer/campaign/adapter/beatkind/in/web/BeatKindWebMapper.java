package com.campaignorganizer.campaign.adapter.beatkind.in.web;

import com.campaignorganizer.campaign.adapter.beatkind.in.web.BeatKindWebDtos.BeatKindRequest;
import com.campaignorganizer.campaign.adapter.beatkind.in.web.BeatKindWebDtos.BeatKindResponse;
import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.CreateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.in.BeatKindCommands.UpdateBeatKindCommand;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BeatKindWebMapper {

    BeatKindResponse toResponse(BeatKindView view);

    default CreateBeatKindCommand toCreateCommand(UUID worldId, BeatKindRequest request) {
        return new CreateBeatKindCommand(worldId, request.name(), request.color());
    }

    default UpdateBeatKindCommand toUpdateCommand(UUID worldId, UUID beatKindId, BeatKindRequest request) {
        return new UpdateBeatKindCommand(worldId, beatKindId, request.name(), request.color());
    }
}
