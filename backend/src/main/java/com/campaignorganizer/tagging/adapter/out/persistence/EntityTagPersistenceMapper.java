package com.campaignorganizer.tagging.adapter.out.persistence;

import com.campaignorganizer.tagging.domain.EntityTag;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface EntityTagPersistenceMapper {

    EntityTagJpaEntity toEntity(EntityTag tag);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default EntityTag toDomain(EntityTagJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return EntityTag.reconstitute(
                entity.getId(),
                entity.getWorldId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getName(),
                entity.getCreatedAt());
    }
}
