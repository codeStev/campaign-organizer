package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.CreateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.UpdateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CreateCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.DeleteCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ListCategoriesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.UpdateCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.CategoryRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryImportPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;
import com.campaignorganizer.worldbuilding.domain.wiki.Category;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Category use cases; also implements the published query port for consumers. */
@Service
public class CategoryService implements CreateCategoryUseCase, UpdateCategoryUseCase,
        DeleteCategoryUseCase, ListCategoriesUseCase, CategoryQueryPort, CategoryImportPort {

    private final CategoryRepositoryPort categories;
    private final WorldExistsPort worlds;
    private final CategoryViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public CategoryService(CategoryRepositoryPort categories, WorldExistsPort worlds,
                           CategoryViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.categories = categories;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CategoryView create(CreateCategoryCommand command) {
        requireWorld(command.worldId());
        requireParent(command.worldId(), command.parentId(), null);
        Category created = Category.create(ids.newId(), command.worldId(), command.parentId(),
                command.name(), clock.instant());
        return viewMapper.toView(categories.save(created));
    }

    @Override
    @Transactional
    public CategoryView update(UpdateCategoryCommand command) {
        Category category = require(command.worldId(), command.categoryId());
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
    public List<CategoryView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public CategoryView importCategory(CategoryView view) {
        Category category = Category.reconstitute(view.id(), view.worldId(), view.parentId(), view.name(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(categories.save(category));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<CategoryView> findByWorld(UUID worldId) {
        return categories.findByWorldOrderByName(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return categories.existsInWorld(categoryId, worldId);
    }

    private Category require(UUID worldId, UUID categoryId) {
        return categories.findByIdAndWorld(categoryId, worldId)
                .orElseThrow(() -> new NotFoundException("Category or world not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("Category or world not found");
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
