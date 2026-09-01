package com.campaignorganizer.tagging.application.port.out;

import com.campaignorganizer.tagging.domain.EntityTag;
import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.UUID;

public interface EntityTagRepositoryPort {

    List<EntityTag> findByWorld(UUID worldId);

    List<EntityTag> findByEntity(UUID worldId, EntityType entityType, UUID entityId);

    List<UUID> findEntityIdsByWorldAndTypeAndName(UUID worldId, EntityType entityType, String name);

    /** Ids of entities whose tag name contains this (already-normalized) fragment. */
    List<UUID> findEntityIdsByWorldAndTypeAndNameContaining(UUID worldId, EntityType entityType,
            String fragment);

    List<String> findDistinctNamesByWorld(UUID worldId);

    EntityTag save(EntityTag tag);

    /** Deletes every existing tag on this entity, for a whole-set replace. */
    void deleteByEntity(UUID worldId, EntityType entityType, UUID entityId);
}
