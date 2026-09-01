package com.campaignorganizer.characters.application.template.service;

import com.campaignorganizer.characters.application.template.port.out.GlobalFieldTemplateRepositoryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serves {@link GlobalFieldTemplateQueryPort} as its own bean, separate from
 * {@link GlobalFieldTemplateService}, to break a Spring bean-construction
 * cycle (ADR-0093): {@code GlobalFieldTemplateService} depends on
 * {@code CharacterSheetTemplateRefPort}/{@code StatblockTemplateRefPort}
 * (implemented by {@code CharacterSheetService}/{@code StatblockService}),
 * which in turn depend on {@code GlobalFieldTemplateQueryPort} to validate
 * template references — if the same service implemented both sides, that's
 * a cycle. This bean depends only on the repository port, same fix already
 * used for {@code TagQueryService} (ADR-0083).
 */
@Service
public class GlobalFieldTemplateQueryService implements GlobalFieldTemplateQueryPort {

    private final GlobalFieldTemplateRepositoryPort templates;
    private final GlobalFieldTemplateViewMapper viewMapper;

    public GlobalFieldTemplateQueryService(GlobalFieldTemplateRepositoryPort templates,
                                           GlobalFieldTemplateViewMapper viewMapper) {
        this.templates = templates;
        this.viewMapper = viewMapper;
    }

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
}
