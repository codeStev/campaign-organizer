package com.campaignorganizer.interchange.foundry.application.port.in;

import com.campaignorganizer.interchange.foundry.domain.FoundryCategoryPushMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Pushes a whole wiki category (and, per {@link FoundryCategoryPushMode}, its subcategories'
 * articles too) to Foundry in one action (ADR-0115 addendum). */
public interface PushCategoryToFoundryUseCase {

    FoundryCategoryPushResult pushCategory(UUID worldId, UUID categoryId, FoundryCategoryPushMode mode);

    /** {@code foundryDocumentId} is the single JournalEntry created in
     * {@link FoundryCategoryPushMode#SINGLE_DOCUMENT} mode, or the category's own Foundry
     * folder id in {@link FoundryCategoryPushMode#FOLDER} mode — either way, the one id worth
     * showing as "what got created" for a last-pushed indicator. */
    record FoundryCategoryPushResult(String foundryDocumentId, Instant pushedAt, int articlesPushed,
                                     List<String> warnings) {
    }
}
