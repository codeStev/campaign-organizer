package com.campaignorganizer.worldbuilding.adapter.timeline.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TimelineWebDtos {

    private TimelineWebDtos() {
    }

    public record TimelineRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 5000) String description,
            UUID calendarId) {
    }

    public record TimelineResponse(
            UUID id,
            UUID worldId,
            String name,
            String description,
            UUID calendarId,
            Instant createdAt,
            Instant updatedAt) {
    }
}
