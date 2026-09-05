package com.campaignorganizer.interchange.calendar.adapter.in.web;

import com.campaignorganizer.interchange.calendar.application.port.in.GetOrCreateCalendarFeedUseCase;
import com.campaignorganizer.interchange.calendar.application.port.in.RegenerateCalendarFeedUseCase;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Get-or-create / regenerate a campaign's .ics subscription token (ADR-0108).
 * The token itself is never exposed on the general Campaign DTO — only here,
 * so it isn't leaked into every campaign list/get response. */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/calendar-feed")
public class CampaignCalendarTokenController {

    private final GetOrCreateCalendarFeedUseCase getOrCreate;
    private final RegenerateCalendarFeedUseCase regenerate;

    public CampaignCalendarTokenController(GetOrCreateCalendarFeedUseCase getOrCreate,
                                           RegenerateCalendarFeedUseCase regenerate) {
        this.getOrCreate = getOrCreate;
        this.regenerate = regenerate;
    }

    @GetMapping
    public CalendarFeedTokenResponse getOrCreate(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return new CalendarFeedTokenResponse(getOrCreate.getOrCreateToken(worldId, campaignId));
    }

    @PostMapping("/regenerate")
    public CalendarFeedTokenResponse regenerate(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return new CalendarFeedTokenResponse(regenerate.regenerateToken(worldId, campaignId));
    }

    public record CalendarFeedTokenResponse(UUID token) {
    }
}
