package com.campaignorganizer.interchange.overview.adapter.in.web;

import com.campaignorganizer.interchange.overview.application.port.in.GetGlobalOverviewUseCase;
import com.campaignorganizer.interchange.overview.application.port.in.GlobalOverviewDtos.GlobalOverviewStats;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Issue #68: account-wide landing page stats. */
@RestController
@RequestMapping("/api/overview")
public class GlobalOverviewController {

    private final GetGlobalOverviewUseCase overview;

    public GlobalOverviewController(GetGlobalOverviewUseCase overview) {
        this.overview = overview;
    }

    @GetMapping
    public GlobalOverviewStats get() {
        return overview.overview();
    }
}
