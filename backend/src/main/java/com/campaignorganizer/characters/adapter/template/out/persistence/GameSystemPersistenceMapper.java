package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.domain.template.GameSystem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameSystemPersistenceMapper {

    GameSystemJpaEntity toEntity(GameSystem system);

    default GameSystem toDomain(GameSystemJpaEntity e) {
        if (e == null) {
            return null;
        }
        return GameSystem.reconstitute(e.getId(), e.getName(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
