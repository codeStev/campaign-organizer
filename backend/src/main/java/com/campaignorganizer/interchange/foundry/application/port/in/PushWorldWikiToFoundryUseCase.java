package com.campaignorganizer.interchange.foundry.application.port.in;

import com.campaignorganizer.interchange.foundry.application.port.in.PushCategoryToFoundryUseCase.FoundryCategoryPushResult;
import java.util.UUID;

/** Pushes every category and article in the world's wiki to Foundry in one action — the
 * world-scoped analogue of {@link PushCategoryToFoundryUseCase} (ADR-0115 addendum). Unlike a
 * single category's push, this is always laid out as nested Foundry folders mirroring the
 * category tree, one JournalEntry per article — never bundled into a single document (the user
 * explicitly did not want a whole-wiki single-document option; that choice stays category-only).
 * Uncategorised articles are included, placed directly in the flat "Articles" folder. */
public interface PushWorldWikiToFoundryUseCase {

    FoundryCategoryPushResult pushWorldWiki(UUID worldId);
}
