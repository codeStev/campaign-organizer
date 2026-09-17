package com.campaignorganizer.interchange.foundry.application.port.in;

import java.util.List;
import java.util.UUID;

/** Bulk-runs {@link PushSessionToFoundryUseCase} across every session in a campaign in one
 * action (ADR-0115 addendum) — same content as pushing each session individually (referenced
 * articles/handouts/roll tables/card decks, plus a per-session Session Guide), just batched.
 * Campaigns don't own wiki content directly in this app's model, so unlike
 * {@link PushCategoryToFoundryUseCase}/{@link PushWorldWikiToFoundryUseCase} there is no
 * separate folder/single-document choice here. */
public interface PushCampaignToFoundryUseCase {

    FoundryCampaignPushResult pushCampaign(UUID worldId, UUID campaignId);

    record FoundryCampaignPushResult(int sessionsPushed, int articlesPushed, int handoutsPushed,
                                     int rollTablesPushed, int cardDecksPushed, int sessionGuidesCreated,
                                     List<String> warnings) {
    }
}
