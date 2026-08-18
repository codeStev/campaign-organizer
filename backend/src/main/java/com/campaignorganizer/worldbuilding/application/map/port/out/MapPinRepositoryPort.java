package com.campaignorganizer.worldbuilding.application.map.port.out;

import com.campaignorganizer.worldbuilding.domain.map.MapPin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MapPinRepositoryPort {

    List<MapPin> findByMap(UUID mapId);

    List<MapPin> findByArticle(UUID articleId);

    Optional<MapPin> findByIdAndMap(UUID pinId, UUID mapId);

    MapPin save(MapPin pin);

    void delete(MapPin pin);
}
