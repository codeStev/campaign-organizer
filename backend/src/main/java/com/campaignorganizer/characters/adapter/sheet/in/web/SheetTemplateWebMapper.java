package com.campaignorganizer.characters.adapter.sheet.in.web;

import com.campaignorganizer.characters.adapter.sheet.in.web.SheetTemplateWebDtos.SheetTemplateRequest;
import com.campaignorganizer.characters.adapter.sheet.in.web.SheetTemplateWebDtos.SheetTemplateResponse;
import com.campaignorganizer.characters.application.sheet.port.in.SheetTemplateCommands.CreateSheetTemplateCommand;
import com.campaignorganizer.characters.application.sheet.port.in.SheetTemplateCommands.UpdateSheetTemplateCommand;
import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SheetTemplateWebMapper {

    SheetTemplateResponse toResponse(SheetTemplateView view);

    default CreateSheetTemplateCommand toCreateCommand(UUID worldId, SheetTemplateRequest request) {
        return new CreateSheetTemplateCommand(worldId, request.name(), request.system(),
                request.sections());
    }

    default UpdateSheetTemplateCommand toUpdateCommand(UUID worldId, UUID templateId,
                                                        SheetTemplateRequest request) {
        return new UpdateSheetTemplateCommand(worldId, templateId, request.name(), request.system(),
                request.sections());
    }
}
