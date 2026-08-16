package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import com.campaignorganizer.worldbuilding.domain.map.MapPin;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapPinPersistenceMapper {

    MapPinJpaEntity toEntity(MapPin pin);

    default MapPin toDomain(MapPinJpaEntity e) {
        if (e == null) {
            return null;
        }
        return MapPin.reconstitute(e.getId(), e.getMapId(), e.getArticleId(), e.getLabel(),
                e.getLayer(), e.getX(), e.getY(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
