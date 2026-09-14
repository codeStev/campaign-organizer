package com.campaignorganizer.characters.application.template.port.out;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.GlobalFieldTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GlobalFieldTemplateRepositoryPort {

    List<GlobalFieldTemplate> findAllByOwnerId(UUID ownerId);

    List<GlobalFieldTemplate> findByOwnerIdAndKind(UUID ownerId, TemplateKind kind);

    Optional<GlobalFieldTemplate> findById(UUID templateId);

    Optional<GlobalFieldTemplate> findByKindAndSystemIdAndName(TemplateKind kind, UUID systemId, String name);

    boolean existsBySystemId(UUID systemId);

    GlobalFieldTemplate save(GlobalFieldTemplate template);

    void delete(GlobalFieldTemplate template);

    /** One-time backfill only (ADR-0109). */
    void assignUnownedTo(UUID ownerId);
}
