package com.campaignorganizer.tagging.application.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tagging.application.port.in.ListEntityTagsUseCase;
import com.campaignorganizer.tagging.application.port.in.ListWorldTagsUseCase;
import com.campaignorganizer.tagging.application.port.in.SetEntityTagsUseCase;
import com.campaignorganizer.tagging.application.port.in.TagCommands.SetEntityTagsCommand;
import com.campaignorganizer.tagging.application.port.out.ArticleExistsPort;
import com.campaignorganizer.tagging.application.port.out.EntityTagRepositoryPort;
import com.campaignorganizer.tagging.application.port.out.StatblockExistsPort;
import com.campaignorganizer.tagging.application.port.out.WorldExistsPort;
import com.campaignorganizer.tagging.application.port.published.TagImportPort;
import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tagging.application.port.published.TagView;
import com.campaignorganizer.tagging.domain.EntityTag;
import com.campaignorganizer.tagging.domain.EntityType;
import java.time.Clock;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tag write use cases (FR-47) plus the published import port. Reads are
 * delegated to {@link TagQueryPort} (implemented by {@link TagQueryService},
 * a separate bean — see its Javadoc for why the query port isn't
 * implemented here directly).
 */
@Service
public class TaggingService implements SetEntityTagsUseCase, ListEntityTagsUseCase,
        ListWorldTagsUseCase, TagImportPort {

    private final EntityTagRepositoryPort tags;
    private final WorldExistsPort worlds;
    private final ArticleExistsPort articles;
    private final StatblockExistsPort statblocks;
    private final TagQueryPort tagQuery;
    private final TagViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public TaggingService(EntityTagRepositoryPort tags, WorldExistsPort worlds,
                          ArticleExistsPort articles, StatblockExistsPort statblocks,
                          TagQueryPort tagQuery, TagViewMapper viewMapper, IdGenerator ids,
                          Clock clock) {
        this.tags = tags;
        this.worlds = worlds;
        this.articles = articles;
        this.statblocks = statblocks;
        this.tagQuery = tagQuery;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<String> set(SetEntityTagsCommand command) {
        requireWorld(command.worldId());
        requireEntity(command.worldId(), command.entityType(), command.entityId());
        tags.deleteByEntity(command.worldId(), command.entityType(), command.entityId());
        TreeSet<String> normalized = new TreeSet<>();
        for (String rawName : command.names()) {
            normalized.add(EntityTag.normalize(rawName));
        }
        for (String name : normalized) {
            tags.save(EntityTag.create(ids.newId(), command.worldId(), command.entityType(),
                    command.entityId(), name, clock.instant()));
        }
        return List.copyOf(normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> list(UUID worldId, EntityType entityType, UUID entityId) {
        requireWorld(worldId);
        requireEntity(worldId, entityType, entityId);
        return tagQuery.tagsFor(worldId, entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> list(UUID worldId) {
        requireWorld(worldId);
        return tagQuery.distinctNames(worldId);
    }

    // --- published import port (FR-36) ---

    @Override
    @Transactional
    public TagView importTag(TagView view) {
        EntityTag tag = EntityTag.reconstitute(view.id(), view.worldId(), view.entityType(),
                view.entityId(), view.name(), view.createdAt());
        return viewMapper.toView(tags.save(tag));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void requireEntity(UUID worldId, EntityType entityType, UUID entityId) {
        boolean exists = switch (entityType) {
            case ARTICLE -> articles.existsInWorld(entityId, worldId);
            case STATBLOCK -> statblocks.existsInWorld(entityId, worldId);
        };
        if (!exists) {
            throw new NotFoundException(entityType + " not found");
        }
    }
}
