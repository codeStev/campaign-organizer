package com.campaignorganizer.characters.application.template.port.out;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.GlobalFieldTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GlobalFieldTemplateRepositoryPort {

    List<GlobalFieldTemplate> findAll();

    List<GlobalFieldTemplate> findByKind(TemplateKind kind);

    Optional<GlobalFieldTemplate> findById(UUID templateId);

    Optional<GlobalFieldTemplate> findByKindAndSystemIdAndName(TemplateKind kind, UUID systemId, String name);

    boolean existsBySystemId(UUID systemId);

    GlobalFieldTemplate save(GlobalFieldTemplate template);

    void delete(GlobalFieldTemplate template);
}
