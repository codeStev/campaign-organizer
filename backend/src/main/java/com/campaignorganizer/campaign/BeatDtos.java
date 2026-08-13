package com.campaignorganizer.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BeatDtos {

    private BeatDtos() {
    }

    public record BeatRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 20000) String body,
            Boolean done,
            List<UUID> articleIds,
            UUID sessionId,
            Integer position) {

        public boolean doneOrDefault() {
            return done != null && done;
        }

        public List<UUID> articleIdsOrEmpty() {
            return articleIds == null ? List.of() : articleIds.stream().distinct().toList();
        }
    }

    public record BeatResponse(
            UUID id,
            UUID arcId,
            String title,
            String body,
            boolean done,
            List<UUID> articleIds,
            UUID sessionId,
            int position,
            Instant createdAt,
            Instant updatedAt) {

        public static BeatResponse from(ArcBeat b) {
            return new BeatResponse(b.getId(), b.getArcId(), b.getTitle(), b.getBody(), b.isDone(),
                    List.copyOf(b.getArticleIds()), b.getSessionId(), b.getPosition(),
                    b.getCreatedAt(), b.getUpdatedAt());
        }
    }
}
