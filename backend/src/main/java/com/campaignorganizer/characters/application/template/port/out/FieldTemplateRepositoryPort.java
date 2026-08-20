package com.campaignorganizer.characters.application.template.port.out;

import com.campaignorganizer.characters.domain.template.FieldTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldTemplateRepositoryPort {

    List<FieldTemplate> findByWorld(UUID worldId);

    Optional<FieldTemplate> findByIdAndWorld(UUID templateId, UUID worldId);

    boolean existsInWorld(UUID templateId, UUID worldId);

    FieldTemplate save(FieldTemplate template);

    void delete(FieldTemplate template);
}
