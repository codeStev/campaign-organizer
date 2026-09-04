package com.campaignorganizer.characters.application.category.service;

import com.campaignorganizer.characters.application.category.port.in.CreateSheetCategoryUseCase;
import com.campaignorganizer.characters.application.category.port.in.DeleteSheetCategoryUseCase;
import com.campaignorganizer.characters.application.category.port.in.ListSheetCategoriesUseCase;
import com.campaignorganizer.characters.application.category.port.in.SheetCategoryCommands.CreateSheetCategoryCommand;
import com.campaignorganizer.characters.application.category.port.in.SheetCategoryCommands.UpdateSheetCategoryCommand;
import com.campaignorganizer.characters.application.category.port.in.UpdateSheetCategoryUseCase;
import com.campaignorganizer.characters.application.category.port.out.SheetCategoryRepositoryPort;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryImportPort;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryQueryPort;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryView;
import com.campaignorganizer.characters.application.sheet.port.out.WorldExistsPort;
import com.campaignorganizer.characters.domain.category.SheetCategory;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sheet category use cases; also implements the published query/import ports. */
@Service
public class SheetCategoryService implements CreateSheetCategoryUseCase, UpdateSheetCategoryUseCase,
        DeleteSheetCategoryUseCase, ListSheetCategoriesUseCase, SheetCategoryQueryPort,
        SheetCategoryImportPort {

    private final SheetCategoryRepositoryPort categories;
    private final WorldExistsPort worlds;
    private final SheetCategoryViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public SheetCategoryService(SheetCategoryRepositoryPort categories, WorldExistsPort worlds,
                           SheetCategoryViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.categories = categories;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SheetCategoryView create(CreateSheetCategoryCommand command) {
        requireWorld(command.worldId());
        requireParent(command.worldId(), command.parentId(), null);
        SheetCategory created = SheetCategory.create(ids.newId(), command.worldId(), command.parentId(),
                command.name(), clock.instant());
        return viewMapper.toView(categories.save(created));
    }

    @Override
    @Transactional
    public SheetCategoryView update(UpdateSheetCategoryCommand command) {
        SheetCategory category = require(command.worldId(), command.categoryId());
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
    public List<SheetCategoryView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public SheetCategoryView importSheetCategory(SheetCategoryView view) {
        SheetCategory category = SheetCategory.reconstitute(view.id(), view.worldId(), view.parentId(),
                view.name(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(categories.save(category));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<SheetCategoryView> findByWorld(UUID worldId) {
        return categories.findByWorldOrderByName(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return categories.existsInWorld(categoryId, worldId);
    }

    private SheetCategory require(UUID worldId, UUID categoryId) {
        return categories.findByIdAndWorld(categoryId, worldId)
                .orElseThrow(() -> new NotFoundException("Sheet category or world not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("Sheet category or world not found");
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
