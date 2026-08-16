package com.campaignorganizer.worldbuilding.application.map.service;

import com.campaignorganizer.media.application.port.published.MediaLookupPort;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.map.port.in.CreateMapUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.DeleteMapUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.GetMapUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.ListMapsUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCommands.CreateMapCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCommands.UpdateMapCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.UpdateMapUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.out.MapRepositoryPort;
import com.campaignorganizer.worldbuilding.application.map.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import com.campaignorganizer.worldbuilding.domain.map.WorldMap;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Map use cases; also implements the published query port for consumers. */
@Service
public class MapService implements CreateMapUseCase, UpdateMapUseCase, DeleteMapUseCase,
        GetMapUseCase, ListMapsUseCase, MapQueryPort {

    private final MapRepositoryPort maps;
    private final WorldExistsPort worlds;
    private final MediaLookupPort media;
    private final MapViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public MapService(MapRepositoryPort maps, WorldExistsPort worlds, MediaLookupPort media,
                      MapViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.maps = maps;
        this.worlds = worlds;
        this.media = media;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MapView create(CreateMapCommand command) {
        requireWorld(command.worldId());
        requireImage(command.worldId(), command.mediaId());
        WorldMap created = WorldMap.create(ids.newId(), command.worldId(), command.name(),
                command.mediaId(), clock.instant());
        return viewMapper.toView(maps.save(created));
    }

    @Override
    @Transactional
    public MapView update(UpdateMapCommand command) {
        WorldMap map = require(command.worldId(), command.mapId());
        requireImage(command.worldId(), command.mediaId());
        map.update(command.name(), command.mediaId(), clock.instant());
        return viewMapper.toView(maps.save(map));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID mapId) {
        maps.delete(require(worldId, mapId));
    }

    @Override
    @Transactional(readOnly = true)
    public MapView get(UUID worldId, UUID mapId) {
        return viewMapper.toView(require(worldId, mapId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapView> list(UUID worldId) {
        requireWorld(worldId);
        return maps.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<MapView> findByWorld(UUID worldId) {
        return maps.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MapView> findByIdInWorld(UUID mapId, UUID worldId) {
        return maps.findByIdAndWorld(mapId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MapView> findById(UUID mapId) {
        return maps.findById(mapId).map(viewMapper::toView);
    }

    private WorldMap require(UUID worldId, UUID mapId) {
        return maps.findByIdAndWorld(mapId, worldId)
                .orElseThrow(() -> new NotFoundException("Map not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void requireImage(UUID worldId, UUID mediaId) {
        if (mediaId == null || !media.existsInWorld(mediaId, worldId)) {
            throw new ValidationException("Image not found in this world");
        }
    }
}
