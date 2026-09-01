package com.campaignorganizer.characters.application.statblock.port.published;

import java.util.UUID;

/** Published port: repoints statblocks' template references (used by template promotion, ADR-0093). */
public interface StatblockTemplateRefPort {

    void repointWorldTemplateToGlobal(UUID worldTemplateId, UUID globalTemplateId);

    boolean existsReferencingGlobalTemplate(UUID globalTemplateId);
}
