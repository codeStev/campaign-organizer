package com.campaignorganizer.worldbuilding.adapter.world.out.persistence;

import com.campaignorganizer.worldbuilding.domain.world.World;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorldPersistenceMapper {

    WorldJpaEntity toEntity(World world);

    default World toDomain(WorldJpaEntity e) {
        if (e == null) {
            return null;
        }
        return World.reconstitute(e.getId(), e.getName(), e.getDescription(), e.getLayerStyles(),
                e.isScratch(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
