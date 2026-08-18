package com.campaignorganizer.worldbuilding.adapter.relationship.in.web;

import com.campaignorganizer.worldbuilding.adapter.relationship.in.web.RelationshipWebDtos.RelationshipRequest;
import com.campaignorganizer.worldbuilding.adapter.relationship.in.web.RelationshipWebDtos.RelationshipResponse;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.RelationshipCommands.CreateRelationshipCommand;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.RelationshipCommands.UpdateRelationshipCommand;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps relationship web DTOs ↔ commands/view (MapStruct). */
@Mapper(componentModel = "spring")
public interface RelationshipWebMapper {

    RelationshipResponse toResponse(RelationshipView view);

    default CreateRelationshipCommand toCreateCommand(UUID worldId, RelationshipRequest request) {
        return new CreateRelationshipCommand(worldId, request.fromArticleId(), request.toArticleId(),
                request.label(), request.directedOrDefault());
    }

    default UpdateRelationshipCommand toUpdateCommand(UUID worldId, UUID relationshipId,
                                                      RelationshipRequest request) {
        return new UpdateRelationshipCommand(worldId, relationshipId, request.fromArticleId(),
                request.toArticleId(), request.label(), request.directedOrDefault());
    }
}
