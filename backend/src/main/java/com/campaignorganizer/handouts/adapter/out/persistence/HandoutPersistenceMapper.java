package com.campaignorganizer.handouts.adapter.out.persistence;

import com.campaignorganizer.handouts.domain.Handout;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface HandoutPersistenceMapper {

    HandoutJpaEntity toEntity(Handout handout);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default Handout toDomain(HandoutJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Handout.reconstitute(
                entity.getId(),
                entity.getWorldId(),
                entity.getTitle(),
                Handout.Preset.valueOf(entity.getPreset()),
                entity.getBody(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
