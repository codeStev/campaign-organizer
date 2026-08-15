package com.campaignorganizer.worldbuilding.adapter.relationship.out.persistence;

import com.campaignorganizer.worldbuilding.domain.relationship.Relationship;
import org.mapstruct.Mapper;

/** Maps the domain relationship to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface RelationshipPersistenceMapper {

    RelationshipJpaEntity toEntity(Relationship relationship);

    default Relationship toDomain(RelationshipJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Relationship.reconstitute(
                entity.getId(),
                entity.getWorldId(),
                entity.getFromArticleId(),
                entity.getToArticleId(),
                entity.getLabel(),
                entity.isDirected(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
