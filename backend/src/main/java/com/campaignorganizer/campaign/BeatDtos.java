package com.campaignorganizer.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class BeatDtos {

    private BeatDtos() {
    }

    public record BeatRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 20000) String body,
            Boolean done,
            UUID articleId,
            UUID sessionId,
            Integer position) {

        public boolean doneOrDefault() {
            return done != null && done;
        }
    }

    public record BeatResponse(
            UUID id,
            UUID arcId,
            String title,
            String body,
            boolean done,
            UUID articleId,
            UUID sessionId,
            int position,
            Instant createdAt,
            Instant updatedAt) {

        public static BeatResponse from(ArcBeat b) {
            return new BeatResponse(b.getId(), b.getArcId(), b.getTitle(), b.getBody(), b.isDone(),
                    b.getArticleId(), b.getSessionId(), b.getPosition(), b.getCreatedAt(), b.getUpdatedAt());
        }
    }
}
