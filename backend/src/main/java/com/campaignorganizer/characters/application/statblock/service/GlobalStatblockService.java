package com.campaignorganizer.characters.application.statblock.service;

import com.campaignorganizer.characters.application.statblock.port.in.CreateGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.CreateStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.DeleteGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.GetGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.CreateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.UpdateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.ImportGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.ListGlobalStatblocksUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.CreateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.UpdateGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.out.GlobalStatblockRepositoryPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockImportPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockOwnershipPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockRefPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.statblock.GlobalStatblock;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.security.CurrentUserPort;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global (catalog) statblock use cases (ADR-0096); also implements the published
 * import/ref/ownership ports. The published query port is served by the separate
 * {@link GlobalStatblockQueryService} bean instead — see its Javadoc for why (a Spring
 * bean-construction cycle through {@code @PreAuthorize}'s AOP infrastructure, ADR-0109).
 */
@Service
public class GlobalStatblockService implements CreateGlobalStatblockUseCase, UpdateGlobalStatblockUseCase,
        DeleteGlobalStatblockUseCase, GetGlobalStatblockUseCase, ListGlobalStatblocksUseCase,
        ImportGlobalStatblockUseCase, GlobalStatblockImportPort,
        GlobalStatblockRefPort, GlobalStatblockOwnershipPort {

    private final GlobalStatblockRepositoryPort statblocks;
    private final GameSystemQueryPort systems;
    private final GlobalFieldTemplateQueryPort templates;
    private final CreateStatblockUseCase createStatblock;
    private final GlobalStatblockViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;
    private final CurrentUserPort currentUser;

    public GlobalStatblockService(GlobalStatblockRepositoryPort statblocks, GameSystemQueryPort systems,
                                  GlobalFieldTemplateQueryPort templates, CreateStatblockUseCase createStatblock,
                                  GlobalStatblockViewMapper viewMapper, IdGenerator ids, Clock clock,
                                  CurrentUserPort currentUser) {
        this.statblocks = statblocks;
        this.systems = systems;
        this.templates = templates;
        this.createStatblock = createStatblock;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#command.systemId(), 'GameSystem', 'ACCESS') "
            + "and (#command.globalTemplateId() == null "
            + "or hasPermission(#command.globalTemplateId(), 'GlobalFieldTemplate', 'ACCESS'))")
    public GlobalStatblockView create(CreateGlobalStatblockCommand command) {
        requireSystem(command.systemId());
        validateTemplate(command.systemId(), command.globalTemplateId());
        GlobalStatblock created = GlobalStatblock.create(ids.newId(), command.systemId(),
                command.globalTemplateId(), command.name(), command.stats(), command.notes(),
                currentUser.currentAccountId(), clock.instant());
        return viewMapper.toView(statblocks.save(created));
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#command.globalStatblockId(), 'GlobalStatblock', 'ACCESS') "
            + "and hasPermission(#command.systemId(), 'GameSystem', 'ACCESS') "
            + "and (#command.globalTemplateId() == null "
            + "or hasPermission(#command.globalTemplateId(), 'GlobalFieldTemplate', 'ACCESS'))")
    public GlobalStatblockView update(UpdateGlobalStatblockCommand command) {
        GlobalStatblock statblock = require(command.globalStatblockId());
        requireSystem(command.systemId());
        validateTemplate(command.systemId(), command.globalTemplateId());
        statblock.update(command.systemId(), command.globalTemplateId(), command.name(), command.stats(),
                command.notes(), clock.instant());
        return viewMapper.toView(statblocks.save(statblock));
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#globalStatblockId, 'GlobalStatblock', 'ACCESS')")
    public void delete(UUID globalStatblockId) {
        statblocks.delete(require(globalStatblockId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#globalStatblockId, 'GlobalStatblock', 'ACCESS')")
    public GlobalStatblockView get(UUID globalStatblockId) {
        return viewMapper.toView(require(globalStatblockId));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("#systemId == null or hasPermission(#systemId, 'GameSystem', 'ACCESS')")
    public List<GlobalStatblockView> list(UUID systemId) {
        List<GlobalStatblock> result = systemId == null
                ? statblocks.findAllByOwnerId(currentUser.currentAccountId())
                : statblocks.findBySystemId(systemId);
        return result.stream().map(viewMapper::toView).toList();
    }

    // --- import (copy) into a campaign (ADR-0096) ---

    @Override
    @Transactional
    // Both ids need checking: this endpoint (POST /api/statblocks/global/{id}/import) is NOT
    // nested under /api/worlds/{worldId}/**, so WorldAccessAuthorizationManager never sees the
    // destination worldId — without this, any account could write a statblock into any other
    // account's world/campaign just by knowing its id (a cross-account IDOR).
    @PreAuthorize("hasPermission(#globalStatblockId, 'GlobalStatblock', 'ACCESS') "
            + "and hasPermission(#worldId, 'World', 'ACCESS')")
    public StatblockView importIntoCampaign(UUID globalStatblockId, UUID worldId, UUID campaignId,
                                            String nameOverride) {
        if (campaignId == null) {
            throw new ValidationException("A campaign is required to import a statblock");
        }
        GlobalStatblock source = require(globalStatblockId);
        String name = nameOverride == null || nameOverride.isBlank() ? source.getName() : nameOverride;
        return createStatblock.create(new CreateStatblockCommand(worldId, null, null, campaignId, null,
                source.getGlobalTemplateId(), name, source.getStats(), source.getNotes()));
    }

    // --- published import port (ADR-0061/ADR-0096): resolve-or-reuse, not blind recreate ---

    @Override
    @Transactional
    public GlobalStatblockView importOrReuse(GlobalStatblockView view) {
        Optional<GlobalStatblock> existing = statblocks.findBySystemIdAndName(view.systemId(), view.name());
        if (existing.isPresent()) {
            return viewMapper.toView(existing.get());
        }
        GlobalStatblock created = GlobalStatblock.reconstitute(view.id(), view.systemId(),
                view.globalTemplateId(), view.name(), view.stats(), view.notes(), currentUser.currentAccountId(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(statblocks.save(created));
    }

    // --- published ownership port (ADR-0109) ---

    @Override
    @Transactional
    public void assignUnownedTo(UUID ownerId) {
        statblocks.assignUnownedTo(ownerId);
    }

    // --- published ref port (used by GlobalFieldTemplateService.delete()) ---

    @Override
    @Transactional(readOnly = true)
    public boolean existsReferencingGlobalTemplate(UUID globalTemplateId) {
        return statblocks.existsByGlobalTemplateId(globalTemplateId);
    }

    private GlobalStatblock require(UUID globalStatblockId) {
        return statblocks.findById(globalStatblockId)
                .orElseThrow(() -> new NotFoundException("Global statblock not found"));
    }

    private void requireSystem(UUID systemId) {
        if (!systems.existsById(systemId)) {
            throw new ValidationException("Game system not found");
        }
    }

    private void validateTemplate(UUID systemId, UUID globalTemplateId) {
        if (globalTemplateId == null) {
            return;
        }
        GlobalFieldTemplateView template = templates.findById(globalTemplateId)
                .orElseThrow(() -> new ValidationException("Global template not found"));
        if (template.kind() != TemplateKind.STATBLOCK) {
            throw new ValidationException("Template is not a statblock template");
        }
        if (!template.systemId().equals(systemId)) {
            throw new ValidationException("Template must belong to the same game system");
        }
    }
}
