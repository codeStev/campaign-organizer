package com.campaignorganizer.interchange.calendar.adapter.in.web;

import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsUseCase;
import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsUseCase.IcsCalendar;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** One-time authenticated .ics download for a campaign's sessions (ADR-0108).
 * See PublicCalendarFeedController for the unauthenticated subscribe URL. */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/calendar.ics")
public class CampaignIcsDownloadController {

    private static final MediaType TEXT_CALENDAR = MediaType.parseMediaType("text/calendar;charset=utf-8");

    private final ExportCampaignIcsUseCase export;

    public CampaignIcsDownloadController(ExportCampaignIcsUseCase export) {
        this.export = export;
    }

    @GetMapping
    public ResponseEntity<byte[]> download(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        IcsCalendar calendar = export.exportForCampaign(worldId, campaignId);
        String filename = slug(calendar.campaignName()) + ".ics";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(TEXT_CALENDAR)
                .body(calendar.icsText().getBytes(StandardCharsets.UTF_8));
    }

    private static String slug(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        return s.isEmpty() ? "campaign" : s;
    }
}
