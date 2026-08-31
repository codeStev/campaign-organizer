package com.campaignorganizer.tagging.application.port.in;

import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.UUID;

public interface ListEntityTagsUseCase {

    /** The entity's tags, alphabetical. */
    List<String> list(UUID worldId, EntityType entityType, UUID entityId);
}
