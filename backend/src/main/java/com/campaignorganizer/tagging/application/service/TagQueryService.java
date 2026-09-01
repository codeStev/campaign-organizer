package com.campaignorganizer.tagging.application.service;

import com.campaignorganizer.tagging.application.port.out.EntityTagRepositoryPort;
import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tagging.application.port.published.TagView;
import com.campaignorganizer.tagging.domain.EntityTag;
import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The published query port, deliberately split into its own bean depending
 * only on {@link EntityTagRepositoryPort} (ADR-0083). {@link TaggingService}
 * depends on {@code worldbuilding}'s and {@code characters}' published ports
 * to validate an entity exists before tagging it; those contexts' own
 * services in turn need {@link TagQueryPort} to filter their lists by tag.
 * Keeping the query port here — with no dependency back into those
 * contexts — breaks what would otherwise be a Spring bean construction
 * cycle through {@code TaggingService}.
 */
@Service
public class TagQueryService implements TagQueryPort {

    private final EntityTagRepositoryPort tags;
    private final TagViewMapper viewMapper;

    public TagQueryService(EntityTagRepositoryPort tags, TagViewMapper viewMapper) {
        this.tags = tags;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagView> findByWorld(UUID worldId) {
        return tags.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> tagsFor(UUID worldId, EntityType entityType, UUID entityId) {
        return tags.findByEntity(worldId, entityType, entityId).stream()
                .map(EntityTag::getName)
                .sorted()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> entityIdsTaggedWith(UUID worldId, EntityType entityType, String name) {
        return tags.findEntityIdsByWorldAndTypeAndName(worldId, entityType,
                EntityTag.normalize(name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> entityIdsWhereTagContains(UUID worldId, EntityType entityType, String fragment) {
        String normalized = fragment.strip().toLowerCase();
        return tags.findEntityIdsByWorldAndTypeAndNameContaining(worldId, entityType, normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> distinctNames(UUID worldId) {
        return tags.findDistinctNamesByWorld(worldId).stream().sorted().toList();
    }
}
