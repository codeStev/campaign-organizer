package com.campaignorganizer.characters.application.template.port.published;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read field templates from sibling aggregates and other contexts (export). */
public interface FieldTemplateQueryPort {

    List<FieldTemplateView> findByWorld(UUID worldId);

    List<FieldTemplateView> findByWorldAndKind(UUID worldId, TemplateKind kind);

    Optional<FieldTemplateView> findByIdInWorld(UUID templateId, UUID worldId);

    boolean existsInWorld(UUID templateId, UUID worldId);
}
