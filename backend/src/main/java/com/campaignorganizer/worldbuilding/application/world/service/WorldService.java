package com.campaignorganizer.worldbuilding.application.world.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.world.port.in.CreateWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.DeleteWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.GetLayerStylesUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.GetWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.ListWorldsUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.ReplaceLayerStylesUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.UpdateWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.WorldCommands.CreateWorldCommand;
import com.campaignorganizer.worldbuilding.application.world.port.in.WorldCommands.UpdateWorldCommand;
import com.campaignorganizer.worldbuilding.application.world.port.out.WorldRepositoryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldImportPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
import com.campaignorganizer.worldbuilding.domain.world.World;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** World use cases; also implements the published query port for every other context. */
@Service
public class WorldService implements CreateWorldUseCase, UpdateWorldUseCase, DeleteWorldUseCase,
        GetWorldUseCase, ListWorldsUseCase, GetLayerStylesUseCase, ReplaceLayerStylesUseCase,
        WorldQueryPort, WorldImportPort {

    private final WorldRepositoryPort worlds;
    private final WorldViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public WorldService(WorldRepositoryPort worlds, WorldViewMapper viewMapper, IdGenerator ids,
                        Clock clock) {
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public WorldView create(CreateWorldCommand command) {
        World created = World.create(ids.newId(), command.name(), command.description(), clock.instant());
        return viewMapper.toView(worlds.save(created));
    }

    @Override
    @Transactional
    public WorldView update(UpdateWorldCommand command) {
        World world = require(command.worldId());
        world.update(command.name(), command.description(), clock.instant());
        return viewMapper.toView(worlds.save(world));
    }

    @Override
    @Transactional
    public void delete(UUID worldId) {
        worlds.delete(require(worldId));
    }

    @Override
    @Transactional(readOnly = true)
    public WorldView get(UUID worldId) {
        return viewMapper.toView(require(worldId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorldView> list() {
        return worlds.findAllOrderByCreatedAtDesc().stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, LayerStyle> getLayerStyles(UUID worldId) {
        return require(worldId).getLayerStyles();
    }

    @Override
    @Transactional
    public Map<String, LayerStyle> replace(UUID worldId, Map<String, LayerStyle> layerStyles) {
        World world = require(worldId);
        world.replaceLayerStyles(layerStyles, clock.instant());
        return worlds.save(world).getLayerStyles();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public WorldView importWorld(WorldView view) {
        World world = World.reconstitute(view.id(), view.name(), view.description(), view.layerStyles(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(worlds.save(world));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID worldId) {
        return worlds.existsById(worldId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorldView> findById(UUID worldId) {
        return worlds.findById(worldId).map(viewMapper::toView);
    }

    private World require(UUID worldId) {
        return worlds.findById(worldId)
                .orElseThrow(() -> new NotFoundException("World not found"));
    }
}
