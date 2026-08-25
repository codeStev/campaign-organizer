package com.campaignorganizer.interchange.usage.adapter.in.web;

import com.campaignorganizer.interchange.usage.application.port.in.ConsistencyDtos.ConsistencyReport;
import com.campaignorganizer.interchange.usage.application.port.in.GetConsistencyReportUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** FR-43: per-world consistency report. */
@RestController
@RequestMapping("/api/worlds/{worldId}/consistency-report")
public class ConsistencyReportController {

    private final GetConsistencyReportUseCase report;

    public ConsistencyReportController(GetConsistencyReportUseCase report) {
        this.report = report;
    }

    @GetMapping
    public ConsistencyReport get(@PathVariable UUID worldId) {
        return report.report(worldId);
    }
}
