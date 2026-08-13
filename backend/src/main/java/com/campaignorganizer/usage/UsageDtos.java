package com.campaignorganizer.usage;

import java.util.List;
import java.util.UUID;

public final class UsageDtos {

    private UsageDtos() {
    }

    /**
     * One reference to an article (ADR-0033). {@code type} is BEAT, MAP_PIN,
     * TIMELINE_EVENT, RELATIONSHIP, CHARACTER_SHEET, STATBLOCK or ARTICLE_LINK.
     * {@code targetId} is an article the client can open (for RELATIONSHIP and
     * ARTICLE_LINK), else null. {@code campaignId}/{@code campaignName} are set
     * for campaign-scoped references.
     */
    public record Usage(String type, String label, UUID targetId, UUID campaignId, String campaignName) {
    }

    public record UsageResponse(List<Usage> usages) {
    }
}
