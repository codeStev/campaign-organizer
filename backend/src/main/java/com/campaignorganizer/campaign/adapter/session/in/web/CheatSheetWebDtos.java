package com.campaignorganizer.campaign.adapter.session.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Web request/response models for session cheat sheets (FR-37). */
public final class CheatSheetWebDtos {

    private CheatSheetWebDtos() {
    }

    public record FragmentRequest(
            @NotBlank String type,
            String text,
            UUID statblockId,
            UUID tableId,
            UUID entryId,
            UUID deckId,
            UUID cardId) {
    }

    public record CheatSheetRequest(
            @NotNull @Valid List<FragmentRequest> fragments) {
    }

    public record FragmentResponse(
            UUID id,
            String type,
            String text,
            UUID statblockId,
            UUID tableId,
            UUID entryId,
            UUID deckId,
            UUID cardId) {
    }

    public record CheatSheetResponse(
            UUID id,
            UUID sessionId,
            List<FragmentResponse> fragments,
            Instant createdAt,
            Instant updatedAt) {
    }
}
