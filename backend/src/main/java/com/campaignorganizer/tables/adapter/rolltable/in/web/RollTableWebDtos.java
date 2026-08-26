package com.campaignorganizer.tables.adapter.rolltable.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Web request/response models for roll tables. */
public final class RollTableWebDtos {

    private RollTableWebDtos() {
    }

    public record EntryDto(
            Integer minResult,
            Integer maxResult,
            @NotBlank String body,
            List<UUID> nestedTableIds,
            List<UUID> nestedDeckIds) {
    }

    public record RollTableRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String description,
            @NotBlank @Size(max = 100) String diceExpression,
            @NotNull @Valid List<EntryDto> entries) {
    }

    public record EntryResponse(UUID id, Integer minResult, Integer maxResult, String body,
                                List<UUID> nestedTableIds, List<UUID> nestedDeckIds) {
    }

    public record RollTableResponse(
            UUID id,
            UUID worldId,
            String title,
            String description,
            String diceExpression,
            int minResult,
            int maxResult,
            List<EntryResponse> entries,
            Instant createdAt,
            Instant updatedAt) {
    }
}
