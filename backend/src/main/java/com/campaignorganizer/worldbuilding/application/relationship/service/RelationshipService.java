package com.campaignorganizer.worldbuilding.application.relationship.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.CreateRelationshipUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.DeleteRelationshipUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.ListArticleRelationshipsUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.ListRelationshipsUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.RelationshipCommands.CreateRelationshipCommand;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.RelationshipCommands.UpdateRelationshipCommand;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.UpdateRelationshipUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.RelationshipRepositoryPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipImportPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipQueryPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import com.campaignorganizer.worldbuilding.domain.relationship.Relationship;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Relationship use cases; also implements the published query port for consumers. */
@Service
public class RelationshipService implements CreateRelationshipUseCase, UpdateRelationshipUseCase,
        DeleteRelationshipUseCase, ListRelationshipsUseCase, ListArticleRelationshipsUseCase,
        RelationshipQueryPort, RelationshipImportPort {

    private final RelationshipRepositoryPort relationships;
    private final ArticleExistsPort articles;
    private final WorldExistsPort worlds;
    private final RelationshipViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public RelationshipService(RelationshipRepositoryPort relationships, ArticleExistsPort articles,
                               WorldExistsPort worlds, RelationshipViewMapper viewMapper,
                               IdGenerator ids, Clock clock) {
        this.relationships = relationships;
        this.articles = articles;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RelationshipView create(CreateRelationshipCommand command) {
        requireWorld(command.worldId());
        requireEndpoints(command.worldId(), command.fromArticleId(), command.toArticleId());
        Relationship created = Relationship.create(ids.newId(), command.worldId(),
                command.fromArticleId(), command.toArticleId(), command.label(), command.directed(),
                clock.instant());
        return viewMapper.toView(relationships.save(created));
    }

    @Override
    @Transactional
    public RelationshipView update(UpdateRelationshipCommand command) {
        Relationship relationship = require(command.worldId(), command.relationshipId());
        requireEndpoints(command.worldId(), command.fromArticleId(), command.toArticleId());
        relationship.update(command.fromArticleId(), command.toArticleId(), command.label(),
                command.directed(), clock.instant());
        return viewMapper.toView(relationships.save(relationship));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID relationshipId) {
        relationships.delete(require(worldId, relationshipId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelationshipView> list(UUID worldId) {
        requireWorld(worldId);
        return relationships.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelationshipView> listForArticle(UUID worldId, UUID articleId) {
        if (!articles.existsInWorld(articleId, worldId)) {
            throw new NotFoundException("Article not found");
        }
        return relationships.findTouchingArticle(worldId, articleId).stream()
                .map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public RelationshipView importRelationship(RelationshipView view) {
        Relationship relationship = Relationship.reconstitute(view.id(), view.worldId(),
                view.fromArticleId(), view.toArticleId(), view.label(), view.directed(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(relationships.save(relationship));
    }

    // --- published query port (consumed by other contexts) ---

    @Override
    @Transactional(readOnly = true)
    public List<RelationshipView> findByWorld(UUID worldId) {
        return relationships.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RelationshipView> findTouchingArticle(UUID worldId, UUID articleId) {
        return relationships.findTouchingArticle(worldId, articleId).stream()
                .map(viewMapper::toView).toList();
    }

    private Relationship require(UUID worldId, UUID relationshipId) {
        return relationships.findByIdAndWorld(relationshipId, worldId)
                .orElseThrow(() -> new NotFoundException("Relationship not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void requireEndpoints(UUID worldId, UUID fromArticleId, UUID toArticleId) {
        if (!articles.existsInWorld(fromArticleId, worldId)
                || !articles.existsInWorld(toArticleId, worldId)) {
            throw new ValidationException("Both articles must exist in this world");
        }
    }
}
