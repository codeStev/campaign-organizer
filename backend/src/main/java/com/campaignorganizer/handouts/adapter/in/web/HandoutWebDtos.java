package com.campaignorganizer.handouts.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Web request/response models for handouts. */
public final class HandoutWebDtos {

    private HandoutWebDtos() {
    }

    public record HandoutRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull String preset,
            String body,
            UUID sessionId,
            Boolean revealed) {

        public boolean revealedOrDefault() {
            return revealed != null && revealed;
        }
    }

    public record HandoutResponse(
            UUID id,
            UUID worldId,
            String title,
            String preset,
            String body,
            UUID sessionId,
            boolean revealed,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ReorderHandoutsRequest(@NotEmpty List<UUID> orderedIds) {
    }
}
