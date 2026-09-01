package com.campaignorganizer.campaign.adapter.todo.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TodoWebDtos {

    private TodoWebDtos() {
    }

    public record TodoRequest(@NotBlank @Size(max = 2000) String text) {
    }

    public record TodoUpdateRequest(@NotBlank @Size(max = 2000) String text, boolean done) {
    }

    public record TodoResponse(
            UUID id,
            UUID campaignId,
            UUID sessionId,
            String text,
            boolean done,
            Instant createdAt,
            Instant updatedAt) {
    }
}
