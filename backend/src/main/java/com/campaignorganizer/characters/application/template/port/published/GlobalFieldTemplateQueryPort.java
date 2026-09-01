package com.campaignorganizer.characters.application.template.port.published;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read the global field template catalog from sibling aggregates (ADR-0093). */
public interface GlobalFieldTemplateQueryPort {

    List<GlobalFieldTemplateView> findAll();

    List<GlobalFieldTemplateView> findByKind(TemplateKind kind);

    Optional<GlobalFieldTemplateView> findById(UUID templateId);

    boolean existsById(UUID templateId);
}
