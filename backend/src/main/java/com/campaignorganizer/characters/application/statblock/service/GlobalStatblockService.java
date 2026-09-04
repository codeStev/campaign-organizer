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
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockRefPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.statblock.GlobalStatblock;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global (catalog) statblock use cases (ADR-0096); also implements the
 * published import/query/ref ports. Unlike {@code GlobalFieldTemplateService}
 * (ADR-0093), no separate query-service bean is needed — nothing this service
 * depends on ({@code GameSystemQueryPort}, {@code GlobalFieldTemplateQueryPort},
 * {@code CreateStatblockUseCase}) depends back on {@code GlobalStatblockQueryPort}
 * or {@code GlobalStatblockRefPort}, so there's no Spring bean-construction cycle.
 */
@Service
public class GlobalStatblockService implements CreateGlobalStatblockUseCase, UpdateGlobalStatblockUseCase,
        DeleteGlobalStatblockUseCase, GetGlobalStatblockUseCase, ListGlobalStatblocksUseCase,
        ImportGlobalStatblockUseCase, GlobalStatblockImportPort, GlobalStatblockQueryPort,
        GlobalStatblockRefPort {

    private final GlobalStatblockRepositoryPort statblocks;
    private final GameSystemQueryPort systems;
    private final GlobalFieldTemplateQueryPort templates;
    private final CreateStatblockUseCase createStatblock;
    private final GlobalStatblockViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public GlobalStatblockService(GlobalStatblockRepositoryPort statblocks, GameSystemQueryPort systems,
                                  GlobalFieldTemplateQueryPort templates, CreateStatblockUseCase createStatblock,
                                  GlobalStatblockViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.statblocks = statblocks;
        this.systems = systems;
        this.templates = templates;
        this.createStatblock = createStatblock;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GlobalStatblockView create(CreateGlobalStatblockCommand command) {
        requireSystem(command.systemId());
        validateTemplate(command.systemId(), command.globalTemplateId());
        GlobalStatblock created = GlobalStatblock.create(ids.newId(), command.systemId(),
                command.globalTemplateId(), command.name(), command.stats(), command.notes(), clock.instant());
        return viewMapper.toView(statblocks.save(created));
    }

    @Override
    @Transactional
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
    public void delete(UUID globalStatblockId) {
        statblocks.delete(require(globalStatblockId));
    }

    @Override
    @Transactional(readOnly = true)
    public GlobalStatblockView get(UUID globalStatblockId) {
        return viewMapper.toView(require(globalStatblockId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GlobalStatblockView> list(UUID systemId) {
        List<GlobalStatblock> result =
                systemId == null ? statblocks.findAll() : statblocks.findBySystemId(systemId);
        return result.stream().map(viewMapper::toView).toList();
    }

    // --- import (copy) into a campaign (ADR-0096) ---

    @Override
    @Transactional
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
                view.globalTemplateId(), view.name(), view.stats(), view.notes(), view.createdAt(),
                view.updatedAt());
        return viewMapper.toView(statblocks.save(created));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<GlobalStatblockView> findAll() {
        return statblocks.findAll().stream().map(viewMapper::toView).toList();
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
