package com.campaignorganizer.characters.application.statblock.port.published;

import java.util.UUID;

/**
 * Published port: whether any global (catalog) statblock still references a
 * global field template (ADR-0096) — used by {@code GlobalFieldTemplateService
 * .delete()} so deleting an in-use template surfaces a friendly
 * {@code ConflictException} instead of a raw DB FK violation. Mirrors
 * {@code StatblockTemplateRefPort}'s existing reference check.
 */
public interface GlobalStatblockRefPort {

    boolean existsReferencingGlobalTemplate(UUID globalTemplateId);
}
