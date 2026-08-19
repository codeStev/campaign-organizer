package com.campaignorganizer.characters.application.sheet.port.out;

import com.campaignorganizer.characters.domain.sheet.SheetTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SheetTemplateRepositoryPort {

    List<SheetTemplate> findByWorld(UUID worldId);

    Optional<SheetTemplate> findByIdAndWorld(UUID templateId, UUID worldId);

    boolean existsInWorld(UUID templateId, UUID worldId);

    SheetTemplate save(SheetTemplate template);

    void delete(SheetTemplate template);
}
