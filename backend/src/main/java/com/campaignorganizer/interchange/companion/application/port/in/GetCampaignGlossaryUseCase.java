package com.campaignorganizer.interchange.companion.application.port.in;

import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryTerm;
import java.util.List;
import java.util.UUID;

/**
 * The proper nouns tied to one campaign (ADR-0120). A campaign the account does not own is reported
 * as not found.
 */
public interface GetCampaignGlossaryUseCase {

    CampaignGlossary get(UUID accountId, UUID campaignId);

    record CampaignGlossary(UUID campaignId, String campaignName, List<GlossaryTerm> terms) {
    }
}
