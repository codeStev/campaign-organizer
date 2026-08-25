package com.campaignorganizer.tables.adapter.rolltable.in.web;

import com.campaignorganizer.tables.adapter.rolltable.in.web.RollTableWebDtos.EntryDto;
import com.campaignorganizer.tables.adapter.rolltable.in.web.RollTableWebDtos.EntryResponse;
import com.campaignorganizer.tables.adapter.rolltable.in.web.RollTableWebDtos.RollTableRequest;
import com.campaignorganizer.tables.adapter.rolltable.in.web.RollTableWebDtos.RollTableResponse;
import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.CreateRollTableCommand;
import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.EntryInput;
import com.campaignorganizer.tables.application.rolltable.port.in.RollTableCommands.UpdateRollTableCommand;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableEntryView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps roll-table web DTOs ↔ commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface RollTableWebMapper {

    RollTableResponse toResponse(RollTableView view);

    EntryResponse toEntryResponse(RollTableEntryView view);

    EntryInput toEntryInput(EntryDto dto);

    List<EntryInput> toEntryInputs(List<EntryDto> entries);

    default CreateRollTableCommand toCreateCommand(UUID worldId, RollTableRequest request) {
        return new CreateRollTableCommand(worldId, request.title(), request.description(),
                request.diceExpression(), toEntryInputs(request.entries()));
    }

    default UpdateRollTableCommand toUpdateCommand(UUID worldId, UUID tableId, RollTableRequest request) {
        return new UpdateRollTableCommand(worldId, tableId, request.title(), request.description(),
                request.diceExpression(), toEntryInputs(request.entries()));
    }
}
