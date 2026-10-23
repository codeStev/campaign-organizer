package com.campaignorganizer.interchange.companion.adapter.in.web;

import java.util.List;
import java.util.UUID;

/** Response payloads for the companion API (mirrors docs/api/openapi.yaml). */
public final class CompanionWebDtos {

    private CompanionWebDtos() {
    }

    public record CompanionCampaignResponse(UUID id, String name, String status, UUID worldId, String worldName) {
    }

    public record GlossaryTermResponse(String name, String kind, List<String> aliases) {
    }

    public record CampaignGlossaryResponse(UUID campaignId, String campaignName, List<GlossaryTermResponse> terms) {
    }
}
