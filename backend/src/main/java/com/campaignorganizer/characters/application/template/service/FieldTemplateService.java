package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.template.port.in.CreateFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.DeleteFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.GetFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.ListFieldTemplatesUseCase;
import com.campaignorganizer.characters.application.template.port.in.FieldTemplateCommands.CreateFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.in.FieldTemplateCommands.UpdateFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.in.UpdateFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.out.FieldTemplateRepositoryPort;
import com.campaignorganizer.characters.application.sheet.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldTemplate;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Field template use cases; also implements the published query port for consumers. */
@Service
public class FieldTemplateService implements CreateFieldTemplateUseCase, UpdateFieldTemplateUseCase,
        DeleteFieldTemplateUseCase, GetFieldTemplateUseCase, ListFieldTemplatesUseCase,
        FieldTemplateQueryPort {

    private final FieldTemplateRepositoryPort templates;
    private final WorldExistsPort worlds;
    private final FieldTemplateViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public FieldTemplateService(FieldTemplateRepositoryPort templates, WorldExistsPort worlds,
                                FieldTemplateViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.templates = templates;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FieldTemplateView create(CreateFieldTemplateCommand command) {
        requireWorld(command.worldId());
        FieldTemplate created = FieldTemplate.create(ids.newId(), command.worldId(), command.name(),
                command.system(), command.sections(), clock.instant());
        return viewMapper.toView(templates.save(created));
    }

    @Override
    @Transactional
    public FieldTemplateView update(UpdateFieldTemplateCommand command) {
        FieldTemplate template = require(command.worldId(), command.templateId());
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
    public FieldTemplateView get(UUID worldId, UUID templateId) {
        return viewMapper.toView(require(worldId, templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldTemplateView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<FieldTemplateView> findByWorld(UUID worldId) {
        return templates.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldTemplateView> findByIdInWorld(UUID templateId, UUID worldId) {
        return templates.findByIdAndWorld(templateId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID templateId, UUID worldId) {
        return templates.existsInWorld(templateId, worldId);
    }

    private FieldTemplate require(UUID worldId, UUID templateId) {
        return templates.findByIdAndWorld(templateId, worldId)
                .orElseThrow(() -> new NotFoundException("Field template not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
