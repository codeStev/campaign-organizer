package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import com.campaignorganizer.worldbuilding.domain.map.WorldMap;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapPersistenceMapper {

    WorldMapJpaEntity toEntity(WorldMap map);

    default WorldMap toDomain(WorldMapJpaEntity e) {
        if (e == null) {
            return null;
        }
        return WorldMap.reconstitute(e.getId(), e.getWorldId(), e.getCategoryId(), e.getName(), e.getMediaId(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
