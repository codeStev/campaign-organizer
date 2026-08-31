package com.campaignorganizer.campaign.adapter.clock.in.web;

import com.campaignorganizer.campaign.adapter.clock.in.web.ClockWebDtos.ClockRequest;
import com.campaignorganizer.campaign.adapter.clock.in.web.ClockWebDtos.ClockResponse;
import com.campaignorganizer.campaign.adapter.clock.in.web.ClockWebDtos.ClockSegmentDto;
import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.CreateClockCommand;
import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.SegmentInput;
import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.UpdateClockCommand;
import com.campaignorganizer.campaign.application.clock.port.published.ClockSegmentView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps clock web DTOs to/from commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface ClockWebMapper {

    ClockResponse toResponse(ClockView view);

    ClockSegmentDto toSegmentResponse(ClockSegmentView view);

    SegmentInput toSegmentInput(ClockSegmentDto dto);

    List<SegmentInput> toSegmentInputs(List<ClockSegmentDto> segments);

    default CreateClockCommand toCreateCommand(UUID worldId, UUID campaignId, ClockRequest request) {
        return new CreateClockCommand(worldId, campaignId, request.title(), request.description(),
                toSegmentInputs(request.segments()), request.position());
    }

    default UpdateClockCommand toUpdateCommand(UUID worldId, UUID campaignId, UUID clockId,
                                               ClockRequest request) {
        return new UpdateClockCommand(worldId, campaignId, clockId, request.title(), request.description(),
                toSegmentInputs(request.segments()), request.position());
    }
}
