package com.campaignorganizer.characters.application.sheet.port.published;

import java.util.UUID;

/** Published port: repoints character sheets' template references (used by template promotion, ADR-0093). */
public interface CharacterSheetTemplateRefPort {

    void repointWorldTemplateToGlobal(UUID worldTemplateId, UUID globalTemplateId);

    boolean existsReferencingGlobalTemplate(UUID globalTemplateId);
}
