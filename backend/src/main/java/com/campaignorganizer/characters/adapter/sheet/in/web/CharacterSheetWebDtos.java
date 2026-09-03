package com.campaignorganizer.characters.adapter.sheet.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CharacterSheetWebDtos {

    private CharacterSheetWebDtos() {
    }

    public record CharacterSheetRequest(
            UUID categoryId,
            @NotBlank @Size(max = 200) String name,
            UUID worldTemplateId,
            UUID globalTemplateId,
            UUID articleId,
            UUID campaignId,
            Map<String, Object> values) {
    }

    public record CharacterSheetResponse(
            UUID id,
            UUID worldId,
            UUID categoryId,
            UUID worldTemplateId,
            UUID globalTemplateId,
            UUID articleId,
            UUID campaignId,
            String name,
            Map<String, Object> values,
            Instant createdAt,
            Instant updatedAt) {
    }
}
