package com.campaignorganizer.worldbuilding.application.map.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.map.port.in.CreateMapCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.DeleteMapCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.ListMapCategoriesUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCategoryCommands.CreateMapCategoryCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCategoryCommands.UpdateMapCategoryCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.UpdateMapCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.out.MapCategoryRepositoryPort;
import com.campaignorganizer.worldbuilding.application.map.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryImportPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryView;
import com.campaignorganizer.worldbuilding.domain.map.MapCategory;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Map category use cases; also implements the published query port for consumers. */
@Service
public class MapCategoryService implements CreateMapCategoryUseCase, UpdateMapCategoryUseCase,
        DeleteMapCategoryUseCase, ListMapCategoriesUseCase, MapCategoryQueryPort, MapCategoryImportPort {

    private final MapCategoryRepositoryPort categories;
    private final WorldExistsPort worlds;
    private final MapCategoryViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public MapCategoryService(MapCategoryRepositoryPort categories, WorldExistsPort worlds,
                           MapCategoryViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.categories = categories;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MapCategoryView create(CreateMapCategoryCommand command) {
        requireWorld(command.worldId());
        requireParent(command.worldId(), command.parentId(), null);
        MapCategory created = MapCategory.create(ids.newId(), command.worldId(), command.parentId(),
                command.name(), clock.instant());
        return viewMapper.toView(categories.save(created));
    }

    @Override
    @Transactional
    public MapCategoryView update(UpdateMapCategoryCommand command) {
        MapCategory category = require(command.worldId(), command.categoryId());
        requireParent(command.worldId(), command.parentId(), command.categoryId());
        category.update(command.parentId(), command.name(), clock.instant());
        return viewMapper.toView(categories.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID categoryId) {
        categories.delete(require(worldId, categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapCategoryView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public MapCategoryView importMapCategory(MapCategoryView view) {
        MapCategory category = MapCategory.reconstitute(view.id(), view.worldId(), view.parentId(), view.name(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(categories.save(category));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<MapCategoryView> findByWorld(UUID worldId) {
        return categories.findByWorldOrderByName(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return categories.existsInWorld(categoryId, worldId);
    }

    private MapCategory require(UUID worldId, UUID categoryId) {
        return categories.findByIdAndWorld(categoryId, worldId)
                .orElseThrow(() -> new NotFoundException("Map category or world not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("Map category or world not found");
        }
    }

    private void requireParent(UUID worldId, UUID parentId, UUID selfId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new ValidationException("A category cannot be its own parent");
        }
        if (!categories.existsInWorld(parentId, worldId)) {
            throw new ValidationException("Parent category not found in this world");
        }
    }
}
