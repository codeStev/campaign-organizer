package com.campaignorganizer.characters.adapter.document.in.web;

import com.campaignorganizer.characters.adapter.document.in.web.DocumentWebDtos.DocumentRequest;
import com.campaignorganizer.characters.adapter.document.in.web.DocumentWebDtos.DocumentResponse;
import com.campaignorganizer.characters.application.document.port.in.DocumentCommands.CreateDocumentCommand;
import com.campaignorganizer.characters.application.document.port.in.DocumentCommands.UpdateDocumentCommand;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentWebMapper {

    DocumentResponse toResponse(DocumentView view);

    default CreateDocumentCommand toCreateCommand(UUID worldId, DocumentRequest request) {
        return new CreateDocumentCommand(worldId, request.templateId(), request.campaignId(),
                request.name(), request.values());
    }

    default UpdateDocumentCommand toUpdateCommand(UUID worldId, UUID documentId,
                                                   DocumentRequest request) {
        return new UpdateDocumentCommand(worldId, documentId, request.templateId(),
                request.campaignId(), request.name(), request.values());
    }
}
