package com.campaignorganizer.interchange.foundry.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PushArticleToFoundryUseCase {

    FoundryPushResult push(UUID worldId, UUID articleId);

    /** {@code warnings} carries non-fatal issues from the push (e.g. an oversized
     * embedded image that was skipped rather than uploaded) so the UI can show
     * them instead of the push silently "succeeding" with something missing. */
    record FoundryPushResult(String foundryDocumentId, Instant pushedAt, List<String> warnings) {
    }
}
