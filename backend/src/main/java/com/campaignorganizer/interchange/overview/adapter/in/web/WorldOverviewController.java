package com.campaignorganizer.interchange.overview.adapter.in.web;

import com.campaignorganizer.interchange.overview.application.port.in.GetWorldOverviewUseCase;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.WorldOverviewStats;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-62: per-world overview stats. */
@RestController
@RequestMapping("/api/worlds/{worldId}/overview")
public class WorldOverviewController {

    private final GetWorldOverviewUseCase overview;

    public WorldOverviewController(GetWorldOverviewUseCase overview) {
        this.overview = overview;
    }

    @GetMapping
    public WorldOverviewStats get(@PathVariable UUID worldId) {
        return overview.overview(worldId);
    }
}
