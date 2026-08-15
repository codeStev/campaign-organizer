package com.campaignorganizer.worldbuilding.adapter.timeline.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TimelineEventWebDtos {

    private TimelineEventWebDtos() {
    }

    public record TimelineEventRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            UUID articleId,
            @NotNull Integer year,
            @Positive Integer month,
            @Positive Integer day) {
    }

    public record TimelineEventResponse(
            UUID id,
            UUID timelineId,
            UUID articleId,
            String title,
            String description,
            int year,
            Integer month,
            Integer day,
            Instant createdAt,
            Instant updatedAt) {
    }
}
