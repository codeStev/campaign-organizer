package com.campaignorganizer.characters.application.sheet.service;

import com.campaignorganizer.characters.application.sheet.port.in.CreateSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.DeleteSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.GetSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.ListSheetTemplatesUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.SheetTemplateCommands.CreateSheetTemplateCommand;
import com.campaignorganizer.characters.application.sheet.port.in.SheetTemplateCommands.UpdateSheetTemplateCommand;
import com.campaignorganizer.characters.application.sheet.port.in.UpdateSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.out.SheetTemplateRepositoryPort;
import com.campaignorganizer.characters.application.sheet.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;
import com.campaignorganizer.characters.domain.sheet.SheetTemplate;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sheet template use cases; also implements the published query port for consumers. */
@Service
public class SheetTemplateService implements CreateSheetTemplateUseCase, UpdateSheetTemplateUseCase,
        DeleteSheetTemplateUseCase, GetSheetTemplateUseCase, ListSheetTemplatesUseCase,
        SheetTemplateQueryPort {

    private final SheetTemplateRepositoryPort templates;
    private final WorldExistsPort worlds;
    private final SheetTemplateViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public SheetTemplateService(SheetTemplateRepositoryPort templates, WorldExistsPort worlds,
                                SheetTemplateViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.templates = templates;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SheetTemplateView create(CreateSheetTemplateCommand command) {
        requireWorld(command.worldId());
        SheetTemplate created = SheetTemplate.create(ids.newId(), command.worldId(), command.name(),
                command.system(), command.sections(), clock.instant());
        return viewMapper.toView(templates.save(created));
    }

    @Override
    @Transactional
    public SheetTemplateView update(UpdateSheetTemplateCommand command) {
        SheetTemplate template = require(command.worldId(), command.templateId());
        template.update(command.name(), command.system(), command.sections(), clock.instant());
        return viewMapper.toView(templates.save(template));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID templateId) {
        templates.delete(require(worldId, templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public SheetTemplateView get(UUID worldId, UUID templateId) {
        return viewMapper.toView(require(worldId, templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SheetTemplateView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<SheetTemplateView> findByWorld(UUID worldId) {
        return templates.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SheetTemplateView> findByIdInWorld(UUID templateId, UUID worldId) {
        return templates.findByIdAndWorld(templateId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID templateId, UUID worldId) {
        return templates.existsInWorld(templateId, worldId);
    }

    private SheetTemplate require(UUID worldId, UUID templateId) {
        return templates.findByIdAndWorld(templateId, worldId)
                .orElseThrow(() -> new NotFoundException("Sheet template not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
