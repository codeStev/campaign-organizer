package com.campaignorganizer.characters.application.sheet.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read sheet templates from sibling aggregates and other contexts (export). */
public interface SheetTemplateQueryPort {

    List<SheetTemplateView> findByWorld(UUID worldId);

    Optional<SheetTemplateView> findByIdInWorld(UUID templateId, UUID worldId);

    boolean existsInWorld(UUID templateId, UUID worldId);
}
