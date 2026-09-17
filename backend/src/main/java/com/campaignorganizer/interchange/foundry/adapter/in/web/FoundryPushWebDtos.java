package com.campaignorganizer.interchange.foundry.adapter.in.web;

import java.time.Instant;
import java.util.List;

public final class FoundryPushWebDtos {

    private FoundryPushWebDtos() {
    }

    public record FoundryPushResponse(String foundryDocumentId, Instant pushedAt, List<String> warnings) {
    }

    public record FoundryPushStatusResponse(boolean pushed, String foundryDocumentId, Instant pushedAt) {
    }

    public record FoundrySessionPushResponse(int articlesPushed, int handoutsPushed, int rollTablesPushed,
                                             int cardDecksPushed, String sessionGuideDocumentId, int beatsIncluded,
                                             List<String> warnings) {
    }

    public record FoundryCategoryPushResponse(String foundryDocumentId, Instant pushedAt, int articlesPushed,
                                              List<String> warnings) {
    }

    public record FoundryCampaignPushResponse(int sessionsPushed, int articlesPushed, int handoutsPushed,
                                              int rollTablesPushed, int cardDecksPushed, int sessionGuidesCreated,
                                              List<String> warnings) {
    }
}
