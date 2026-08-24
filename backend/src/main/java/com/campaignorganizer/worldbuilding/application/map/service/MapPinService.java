package com.campaignorganizer.worldbuilding.application.map.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.map.port.in.CreateMapPinUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.DeleteMapPinUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.ListMapPinsUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapPinCommands.CreateMapPinCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapPinCommands.UpdateMapPinCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.UpdateMapPinUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.map.port.out.MapPinRepositoryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinImportPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapQueryPort;
import com.campaignorganizer.worldbuilding.domain.map.MapPin;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Map-pin use cases; also implements the published query port for consumers. */
@Service
public class MapPinService implements CreateMapPinUseCase, UpdateMapPinUseCase, DeleteMapPinUseCase,
        ListMapPinsUseCase, MapPinQueryPort, MapPinImportPort {

    private final MapPinRepositoryPort pins;
    private final MapQueryPort maps;
    private final ArticleExistsPort articles;
    private final MapPinViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public MapPinService(MapPinRepositoryPort pins, MapQueryPort maps, ArticleExistsPort articles,
                         MapPinViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.pins = pins;
        this.maps = maps;
        this.articles = articles;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MapPinView create(CreateMapPinCommand command) {
        requireMap(command.worldId(), command.mapId());
        requireArticle(command.worldId(), command.articleId());
        MapPin created = MapPin.create(ids.newId(), command.mapId(), command.articleId(),
                command.label(), command.layer(), command.x(), command.y(), clock.instant());
        return viewMapper.toView(pins.save(created));
    }

    @Override
    @Transactional
    public MapPinView update(UpdateMapPinCommand command) {
        requireMap(command.worldId(), command.mapId());
        MapPin pin = pins.findByIdAndMap(command.pinId(), command.mapId())
                .orElseThrow(() -> new NotFoundException("Pin not found"));
        requireArticle(command.worldId(), command.articleId());
        pin.update(command.articleId(), command.label(), command.layer(), command.x(), command.y(),
                clock.instant());
        return viewMapper.toView(pins.save(pin));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID mapId, UUID pinId) {
        requireMap(worldId, mapId);
        MapPin pin = pins.findByIdAndMap(pinId, mapId)
                .orElseThrow(() -> new NotFoundException("Pin not found"));
        pins.delete(pin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapPinView> list(UUID worldId, UUID mapId) {
        requireMap(worldId, mapId);
        return pins.findByMap(mapId).stream().map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public MapPinView importMapPin(MapPinView view) {
        MapPin pin = MapPin.reconstitute(view.id(), view.mapId(), view.articleId(), view.label(),
                view.layer(), view.x(), view.y(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(pins.save(pin));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<MapPinView> findByMap(UUID mapId) {
        return pins.findByMap(mapId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapPinView> findByArticle(UUID articleId) {
        return pins.findByArticle(articleId).stream().map(viewMapper::toView).toList();
    }

    private void requireMap(UUID worldId, UUID mapId) {
        if (maps.findByIdInWorld(mapId, worldId).isEmpty()) {
            throw new NotFoundException("Map not found");
        }
    }

    private void requireArticle(UUID worldId, UUID articleId) {
        if (articleId != null && !articles.existsInWorld(articleId, worldId)) {
            throw new ValidationException("Article not found in this world");
        }
    }
}
