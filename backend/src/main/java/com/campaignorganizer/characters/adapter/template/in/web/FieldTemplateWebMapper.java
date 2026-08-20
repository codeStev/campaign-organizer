package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.adapter.template.in.web.FieldTemplateWebDtos.FieldTemplateRequest;
import com.campaignorganizer.characters.adapter.template.in.web.FieldTemplateWebDtos.FieldTemplateResponse;
import com.campaignorganizer.characters.application.template.port.in.FieldTemplateCommands.CreateFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.in.FieldTemplateCommands.UpdateFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FieldTemplateWebMapper {

    FieldTemplateResponse toResponse(FieldTemplateView view);

    default CreateFieldTemplateCommand toCreateCommand(UUID worldId, FieldTemplateRequest request) {
        return new CreateFieldTemplateCommand(worldId, request.name(), request.system(),
                request.sections());
    }

    default UpdateFieldTemplateCommand toUpdateCommand(UUID worldId, UUID templateId,
                                                        FieldTemplateRequest request) {
        return new UpdateFieldTemplateCommand(worldId, templateId, request.name(), request.system(),
                request.sections());
    }
}
