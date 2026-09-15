package com.campaignorganizer.interchange.overview.adapter.in.web;

import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.CampaignOverviewStats;
import com.campaignorganizer.interchange.overview.application.port.in.GetCampaignOverviewUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Issue #67: per-campaign dashboard stats. */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/overview")
public class CampaignOverviewController {

    private final GetCampaignOverviewUseCase overview;

    public CampaignOverviewController(GetCampaignOverviewUseCase overview) {
        this.overview = overview;
    }

    @GetMapping
    public CampaignOverviewStats get(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return overview.overview(worldId, campaignId);
    }
}
