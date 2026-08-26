package com.campaignorganizer.tables.adapter.carddeck.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Web request/response models for card decks. */
public final class CardDeckWebDtos {

    private CardDeckWebDtos() {
    }

    public record CardDto(@Size(max = 200) String title, @NotBlank String body,
                          List<UUID> nestedTableIds, List<UUID> nestedDeckIds) {
    }

    public record CardDeckRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @NotNull @Valid List<CardDto> cards) {
    }

    public record CardResponse(UUID id, String title, String body,
                               List<UUID> nestedTableIds, List<UUID> nestedDeckIds) {
    }

    public record CardDeckResponse(
            UUID id,
            UUID worldId,
            String title,
            String description,
            List<CardResponse> cards,
            Instant createdAt,
            Instant updatedAt) {
    }
}
