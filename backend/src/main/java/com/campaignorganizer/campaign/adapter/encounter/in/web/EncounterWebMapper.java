package com.campaignorganizer.campaign.adapter.encounter.in.web;

import com.campaignorganizer.campaign.adapter.encounter.in.web.EncounterWebDtos.EncounterEntryDto;
import com.campaignorganizer.campaign.adapter.encounter.in.web.EncounterWebDtos.EncounterRequest;
import com.campaignorganizer.campaign.adapter.encounter.in.web.EncounterWebDtos.EncounterResponse;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.CreateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.EntryInput;
import com.campaignorganizer.campaign.application.encounter.port.in.EncounterCommands.UpdateEncounterCommand;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterEntryView;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps encounter web DTOs to/from commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface EncounterWebMapper {

    EncounterResponse toResponse(EncounterView view);

    EncounterEntryDto toEntryResponse(EncounterEntryView view);

    EntryInput toEntryInput(EncounterEntryDto dto);

    List<EntryInput> toEntryInputs(List<EncounterEntryDto> entries);

    default CreateEncounterCommand toCreateCommand(UUID worldId, UUID campaignId, EncounterRequest request) {
        return new CreateEncounterCommand(worldId, campaignId, request.name(), request.notes(),
                toEntryInputs(request.entries()));
    }

    default UpdateEncounterCommand toUpdateCommand(UUID worldId, UUID campaignId, UUID encounterId,
                                                   EncounterRequest request) {
        return new UpdateEncounterCommand(worldId, campaignId, encounterId, request.name(), request.notes(),
                toEntryInputs(request.entries()));
    }
}
