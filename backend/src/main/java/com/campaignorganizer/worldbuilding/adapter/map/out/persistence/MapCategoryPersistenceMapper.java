package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import com.campaignorganizer.worldbuilding.domain.map.MapCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapCategoryPersistenceMapper {

    MapCategoryJpaEntity toEntity(MapCategory category);

    default MapCategory toDomain(MapCategoryJpaEntity e) {
        if (e == null) {
            return null;
        }
        return MapCategory.reconstitute(e.getId(), e.getWorldId(), e.getParentId(), e.getName(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
