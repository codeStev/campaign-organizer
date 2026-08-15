package com.campaignorganizer.worldbuilding.adapter.relationship.out.persistence;

import com.campaignorganizer.worldbuilding.application.relationship.port.out.RelationshipRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.relationship.Relationship;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the relationship repository port. */
@Component
public class RelationshipPersistenceAdapter implements RelationshipRepositoryPort {

    private final RelationshipJpaRepository repository;
    private final RelationshipPersistenceMapper mapper;

    public RelationshipPersistenceAdapter(RelationshipJpaRepository repository,
                                          RelationshipPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Relationship> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Relationship> findByIdAndWorld(UUID relationshipId, UUID worldId) {
        return repository.findByIdAndWorldId(relationshipId, worldId).map(mapper::toDomain);
    }

    @Override
    public List<Relationship> findTouchingArticle(UUID worldId, UUID articleId) {
        return repository.findTouchingArticle(worldId, articleId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Relationship save(Relationship relationship) {
        return mapper.toDomain(repository.save(mapper.toEntity(relationship)));
    }

    @Override
    public void delete(Relationship relationship) {
        repository.deleteById(relationship.getId());
    }
}
