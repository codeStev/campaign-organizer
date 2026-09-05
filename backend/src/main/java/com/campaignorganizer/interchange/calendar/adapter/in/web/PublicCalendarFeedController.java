package com.campaignorganizer.interchange.calendar.adapter.in.web;

import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsByTokenUseCase;
import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsUseCase.IcsCalendar;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public, unauthenticated .ics subscription feed a calendar app polls
 * (ADR-0108) — addressed by an unguessable per-campaign token, same
 * "security by obscurity, accepted trade-off for a single-user app"
 * precedent as {@code GET /api/media/{id}/content} (ADR-0016). Permitted in
 * SecurityConfig alongside that endpoint.
 */
@RestController
public class PublicCalendarFeedController {

    private static final MediaType TEXT_CALENDAR = MediaType.parseMediaType("text/calendar;charset=utf-8");

    private final ExportCampaignIcsByTokenUseCase export;

    public PublicCalendarFeedController(ExportCampaignIcsByTokenUseCase export) {
        this.export = export;
    }

    @GetMapping("/api/calendar/{token}.ics")
    public ResponseEntity<byte[]> feed(@PathVariable UUID token) {
        IcsCalendar calendar = export.exportByToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(TEXT_CALENDAR)
                .body(calendar.icsText().getBytes(StandardCharsets.UTF_8));
    }
}
