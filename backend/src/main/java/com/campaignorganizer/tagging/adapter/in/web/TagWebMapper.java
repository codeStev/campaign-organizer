package com.campaignorganizer.tagging.adapter.in.web;

import com.campaignorganizer.tagging.adapter.in.web.TagWebDtos.EntityTagsRequest;
import com.campaignorganizer.tagging.adapter.in.web.TagWebDtos.EntityTagsResponse;
import com.campaignorganizer.tagging.application.port.in.TagCommands.SetEntityTagsCommand;
import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps tag web DTOs ↔ commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface TagWebMapper {

    default EntityTagsResponse toResponse(List<String> tags) {
        return new EntityTagsResponse(tags);
    }

    default SetEntityTagsCommand toSetCommand(UUID worldId, EntityType entityType, UUID entityId,
                                              EntityTagsRequest request) {
        return new SetEntityTagsCommand(worldId, entityType, entityId, Set.copyOf(request.tags()));
    }
}
