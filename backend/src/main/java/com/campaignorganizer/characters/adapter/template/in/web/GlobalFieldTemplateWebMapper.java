package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.adapter.template.in.web.GlobalFieldTemplateWebDtos.GlobalFieldTemplateRequest;
import com.campaignorganizer.characters.adapter.template.in.web.GlobalFieldTemplateWebDtos.GlobalFieldTemplateResponse;
import com.campaignorganizer.characters.application.template.port.in.GlobalFieldTemplateCommands.CreateGlobalFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.in.GlobalFieldTemplateCommands.UpdateGlobalFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GlobalFieldTemplateWebMapper {

    GlobalFieldTemplateResponse toResponse(GlobalFieldTemplateView view);

    default CreateGlobalFieldTemplateCommand toCreateCommand(GlobalFieldTemplateRequest request) {
        return new CreateGlobalFieldTemplateCommand(request.name(), request.kind(), request.systemId(),
                request.sections());
    }

    default UpdateGlobalFieldTemplateCommand toUpdateCommand(UUID templateId,
                                                             GlobalFieldTemplateRequest request) {
        return new UpdateGlobalFieldTemplateCommand(templateId, request.name(), request.systemId(),
                request.sections());
    }
}
