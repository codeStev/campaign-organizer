package com.campaignorganizer.tagging.application.port.published;

import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.UUID;

/** Cross-context read access to tags (published; ADR-0083). */
public interface TagQueryPort {

    List<TagView> findByWorld(UUID worldId);

    List<String> tagsFor(UUID worldId, EntityType entityType, UUID entityId);

    /** Ids of entities of this type, in this world, carrying this (already-normalized) tag. */
    List<UUID> entityIdsTaggedWith(UUID worldId, EntityType entityType, String name);

    /** Every distinct tag name in the world, alphabetical. */
    List<String> distinctNames(UUID worldId);
}
