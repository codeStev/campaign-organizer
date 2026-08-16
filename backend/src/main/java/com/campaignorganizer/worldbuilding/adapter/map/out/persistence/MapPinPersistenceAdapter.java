package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import com.campaignorganizer.worldbuilding.application.map.port.out.MapPinRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.map.MapPin;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MapPinPersistenceAdapter implements MapPinRepositoryPort {

    private final MapPinJpaRepository repository;
    private final MapPinPersistenceMapper mapper;

    public MapPinPersistenceAdapter(MapPinJpaRepository repository, MapPinPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<MapPin> findByMap(UUID mapId) {
        return repository.findByMapIdOrderByCreatedAtAsc(mapId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<MapPin> findByArticle(UUID articleId) {
        return repository.findByArticleId(articleId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<MapPin> findByIdAndMap(UUID pinId, UUID mapId) {
        return repository.findByIdAndMapId(pinId, mapId).map(mapper::toDomain);
    }

    @Override
    public MapPin save(MapPin pin) {
        return mapper.toDomain(repository.save(mapper.toEntity(pin)));
    }

    @Override
    public void delete(MapPin pin) {
        repository.deleteById(pin.getId());
    }
}
