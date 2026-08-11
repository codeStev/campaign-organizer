package com.campaignorganizer.timeline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TimelineDtos {

    private TimelineDtos() {
    }

    public record TimelineRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 5000) String description) {
    }

    public record TimelineResponse(
            UUID id,
            UUID worldId,
            String name,
            String description,
            Instant createdAt,
            Instant updatedAt) {

        public static TimelineResponse from(Timeline t) {
            return new TimelineResponse(t.getId(), t.getWorldId(), t.getName(),
                    t.getDescription(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
