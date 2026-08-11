package com.campaignorganizer.timeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TimelineEventDtos {

    private TimelineEventDtos() {
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

        public static TimelineEventResponse from(TimelineEvent e) {
            return new TimelineEventResponse(e.getId(), e.getTimelineId(), e.getArticleId(),
                    e.getTitle(), e.getDescription(), e.getYear(), e.getMonth(), e.getDay(),
                    e.getCreatedAt(), e.getUpdatedAt());
        }
    }
}
