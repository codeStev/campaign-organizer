package com.campaignorganizer.tagging.application.port.in;

import com.campaignorganizer.tagging.domain.EntityType;
import java.util.Set;
import java.util.UUID;

public final class TagCommands {

    private TagCommands() {
    }

    /** Whole-set replace (ADR-0083): the entity's tags become exactly {@code names}. */
    public record SetEntityTagsCommand(UUID worldId, EntityType entityType, UUID entityId,
                                       Set<String> names) {
    }
}
