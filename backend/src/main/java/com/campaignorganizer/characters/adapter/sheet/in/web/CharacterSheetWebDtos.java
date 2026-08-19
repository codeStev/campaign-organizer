package com.campaignorganizer.characters.adapter.sheet.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CharacterSheetWebDtos {

    private CharacterSheetWebDtos() {
    }

    public record CharacterSheetRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull UUID templateId,
            UUID articleId,
            UUID campaignId,
            Map<String, Object> values) {
    }

    public record CharacterSheetResponse(
            UUID id,
            UUID worldId,
            UUID templateId,
            UUID articleId,
            UUID campaignId,
            String name,
            Map<String, Object> values,
            Instant createdAt,
            Instant updatedAt) {
    }
}
