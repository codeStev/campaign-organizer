package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetTemplateRefPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockTemplateRefPort;
import com.campaignorganizer.characters.application.template.port.in.CreateGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.DeleteGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.GetGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.GlobalFieldTemplateCommands.CreateGlobalFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.in.GlobalFieldTemplateCommands.UpdateGlobalFieldTemplateCommand;
import com.campaignorganizer.characters.application.template.port.in.ListGlobalFieldTemplatesUseCase;
import com.campaignorganizer.characters.application.template.port.in.PromoteFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.UpdateGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.out.FieldTemplateRepositoryPort;
import com.campaignorganizer.characters.application.template.port.out.GlobalFieldTemplateRepositoryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateImportPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldTemplate;
import com.campaignorganizer.characters.domain.template.GlobalFieldTemplate;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Global field template use cases (ADR-0093); also implements the published
 * query/import ports and template promotion.
 */
@Service
public class GlobalFieldTemplateService implements CreateGlobalFieldTemplateUseCase,
        UpdateGlobalFieldTemplateUseCase, DeleteGlobalFieldTemplateUseCase, GetGlobalFieldTemplateUseCase,
        ListGlobalFieldTemplatesUseCase, PromoteFieldTemplateUseCase, GlobalFieldTemplateQueryPort,
        GlobalFieldTemplateImportPort {

    private final GlobalFieldTemplateRepositoryPort templates;
    private final FieldTemplateRepositoryPort worldTemplates;
    private final CharacterSheetTemplateRefPort characterSheetRefs;
    private final StatblockTemplateRefPort statblockRefs;
    private final GlobalFieldTemplateViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public GlobalFieldTemplateService(GlobalFieldTemplateRepositoryPort templates,
                                      FieldTemplateRepositoryPort worldTemplates,
                                      CharacterSheetTemplateRefPort characterSheetRefs,
                                      StatblockTemplateRefPort statblockRefs,
                                      GlobalFieldTemplateViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.templates = templates;
        this.worldTemplates = worldTemplates;
        this.characterSheetRefs = characterSheetRefs;
        this.statblockRefs = statblockRefs;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public GlobalFieldTemplateView create(CreateGlobalFieldTemplateCommand command) {
        GlobalFieldTemplate created = GlobalFieldTemplate.create(ids.newId(), command.name(), command.kind(),
                command.system(), command.sections(), clock.instant());
        return viewMapper.toView(templates.save(created));
    }

    @Override
    @Transactional
    public GlobalFieldTemplateView update(UpdateGlobalFieldTemplateCommand command) {
        GlobalFieldTemplate template = require(command.templateId());
        template.update(command.name(), command.system(), command.sections(), clock.instant());
        return viewMapper.toView(templates.save(template));
    }

    @Override
    @Transactional
    public void delete(UUID templateId) {
        GlobalFieldTemplate template = require(templateId);
        if (characterSheetRefs.existsReferencingGlobalTemplate(templateId)
                || statblockRefs.existsReferencingGlobalTemplate(templateId)) {
            throw new ConflictException(
                    "Global template is still referenced by a character sheet or statblock");
        }
        templates.delete(template);
    }

    @Override
    @Transactional(readOnly = true)
    public GlobalFieldTemplateView get(UUID templateId) {
        return viewMapper.toView(require(templateId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GlobalFieldTemplateView> list(TemplateKind kind) {
        return kind == null ? findAll() : findByKind(kind);
    }

    // --- promote a world-scoped template to global (ADR-0093) ---

    @Override
    @Transactional
    public GlobalFieldTemplateView promote(UUID worldId, UUID templateId) {
        FieldTemplate source = worldTemplates.findByIdAndWorld(templateId, worldId)
                .orElseThrow(() -> new NotFoundException("Field template not found"));
        if (source.getKind() == TemplateKind.DOCUMENT) {
            throw new ValidationException("Document templates cannot be promoted to the global catalog");
        }
        GlobalFieldTemplate global = GlobalFieldTemplate.create(ids.newId(), source.getName(),
                source.getKind(), source.getSystem(), source.getSections(), clock.instant());
        GlobalFieldTemplate saved = templates.save(global);

        characterSheetRefs.repointWorldTemplateToGlobal(templateId, saved.getId());
        statblockRefs.repointWorldTemplateToGlobal(templateId, saved.getId());

        worldTemplates.delete(source);
        return viewMapper.toView(saved);
    }

    // --- published import port (ADR-0061/ADR-0093): resolve-or-reuse, not blind recreate ---

    @Override
    @Transactional
    public GlobalFieldTemplateView importOrReuse(GlobalFieldTemplateView view) {
        Optional<GlobalFieldTemplate> existing =
                templates.findByKindAndSystemAndName(view.kind(), view.system(), view.name());
        if (existing.isPresent()) {
            return viewMapper.toView(existing.get());
        }
        GlobalFieldTemplate created = GlobalFieldTemplate.reconstitute(view.id(), view.name(), view.kind(),
                view.system(), view.sections(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(templates.save(created));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<GlobalFieldTemplateView> findAll() {
        return templates.findAll().stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GlobalFieldTemplateView> findByKind(TemplateKind kind) {
        return templates.findByKind(kind).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GlobalFieldTemplateView> findById(UUID templateId) {
        return templates.findById(templateId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID templateId) {
        return templates.findById(templateId).isPresent();
    }

    private GlobalFieldTemplate require(UUID templateId) {
        return templates.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Global field template not found"));
    }
}
