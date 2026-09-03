package com.campaignorganizer.tables.application.category.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.tables.application.category.port.in.CreateTableDeckCategoryUseCase;
import com.campaignorganizer.tables.application.category.port.in.DeleteTableDeckCategoryUseCase;
import com.campaignorganizer.tables.application.category.port.in.ListTableDeckCategoriesUseCase;
import com.campaignorganizer.tables.application.category.port.in.TableDeckCategoryCommands.CreateTableDeckCategoryCommand;
import com.campaignorganizer.tables.application.category.port.in.TableDeckCategoryCommands.UpdateTableDeckCategoryCommand;
import com.campaignorganizer.tables.application.category.port.in.UpdateTableDeckCategoryUseCase;
import com.campaignorganizer.tables.application.category.port.out.TableDeckCategoryRepositoryPort;
import com.campaignorganizer.tables.application.category.port.out.WorldExistsPort;
import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryImportPort;
import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryQueryPort;
import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryView;
import com.campaignorganizer.tables.domain.category.TableDeckCategory;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Table/deck category use cases; also implements the published query/import ports. */
@Service
public class TableDeckCategoryService implements CreateTableDeckCategoryUseCase, UpdateTableDeckCategoryUseCase,
        DeleteTableDeckCategoryUseCase, ListTableDeckCategoriesUseCase, TableDeckCategoryQueryPort,
        TableDeckCategoryImportPort {

    private final TableDeckCategoryRepositoryPort categories;
    private final WorldExistsPort worlds;
    private final TableDeckCategoryViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public TableDeckCategoryService(TableDeckCategoryRepositoryPort categories, WorldExistsPort worlds,
                           TableDeckCategoryViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.categories = categories;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TableDeckCategoryView create(CreateTableDeckCategoryCommand command) {
        requireWorld(command.worldId());
        requireParent(command.worldId(), command.parentId(), null);
        TableDeckCategory created = TableDeckCategory.create(ids.newId(), command.worldId(), command.parentId(),
                command.name(), clock.instant());
        return viewMapper.toView(categories.save(created));
    }

    @Override
    @Transactional
    public TableDeckCategoryView update(UpdateTableDeckCategoryCommand command) {
        TableDeckCategory category = require(command.worldId(), command.categoryId());
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
    public List<TableDeckCategoryView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public TableDeckCategoryView importTableDeckCategory(TableDeckCategoryView view) {
        TableDeckCategory category = TableDeckCategory.reconstitute(view.id(), view.worldId(), view.parentId(),
                view.name(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(categories.save(category));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<TableDeckCategoryView> findByWorld(UUID worldId) {
        return categories.findByWorldOrderByName(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return categories.existsInWorld(categoryId, worldId);
    }

    private TableDeckCategory require(UUID worldId, UUID categoryId) {
        return categories.findByIdAndWorld(categoryId, worldId)
                .orElseThrow(() -> new NotFoundException("Table/deck category or world not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("Table/deck category or world not found");
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
