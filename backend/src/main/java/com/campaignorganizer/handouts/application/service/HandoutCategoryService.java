package com.campaignorganizer.handouts.application.service;

import com.campaignorganizer.handouts.application.port.in.CreateHandoutCategoryUseCase;
import com.campaignorganizer.handouts.application.port.in.DeleteHandoutCategoryUseCase;
import com.campaignorganizer.handouts.application.port.in.HandoutCategoryCommands.CreateHandoutCategoryCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCategoryCommands.UpdateHandoutCategoryCommand;
import com.campaignorganizer.handouts.application.port.in.ListHandoutCategoriesUseCase;
import com.campaignorganizer.handouts.application.port.in.UpdateHandoutCategoryUseCase;
import com.campaignorganizer.handouts.application.port.out.HandoutCategoryRepositoryPort;
import com.campaignorganizer.handouts.application.port.out.WorldExistsPort;
import com.campaignorganizer.handouts.application.port.published.HandoutCategoryImportPort;
import com.campaignorganizer.handouts.application.port.published.HandoutCategoryQueryPort;
import com.campaignorganizer.handouts.application.port.published.HandoutCategoryView;
import com.campaignorganizer.handouts.domain.HandoutCategory;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handout category use cases; also implements the published query/import ports. */
@Service
public class HandoutCategoryService implements CreateHandoutCategoryUseCase, UpdateHandoutCategoryUseCase,
        DeleteHandoutCategoryUseCase, ListHandoutCategoriesUseCase, HandoutCategoryQueryPort,
        HandoutCategoryImportPort {

    private final HandoutCategoryRepositoryPort categories;
    private final WorldExistsPort worlds;
    private final HandoutCategoryViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public HandoutCategoryService(HandoutCategoryRepositoryPort categories, WorldExistsPort worlds,
                           HandoutCategoryViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.categories = categories;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public HandoutCategoryView create(CreateHandoutCategoryCommand command) {
        requireWorld(command.worldId());
        requireParent(command.worldId(), command.parentId(), null);
        HandoutCategory created = HandoutCategory.create(ids.newId(), command.worldId(), command.parentId(),
                command.name(), clock.instant());
        return viewMapper.toView(categories.save(created));
    }

    @Override
    @Transactional
    public HandoutCategoryView update(UpdateHandoutCategoryCommand command) {
        HandoutCategory category = require(command.worldId(), command.categoryId());
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
    public List<HandoutCategoryView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public HandoutCategoryView importHandoutCategory(HandoutCategoryView view) {
        HandoutCategory category = HandoutCategory.reconstitute(view.id(), view.worldId(), view.parentId(),
                view.name(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(categories.save(category));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<HandoutCategoryView> findByWorld(UUID worldId) {
        return categories.findByWorldOrderByName(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return categories.existsInWorld(categoryId, worldId);
    }

    private HandoutCategory require(UUID worldId, UUID categoryId) {
        return categories.findByIdAndWorld(categoryId, worldId)
                .orElseThrow(() -> new NotFoundException("Handout category or world not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("Handout category or world not found");
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
