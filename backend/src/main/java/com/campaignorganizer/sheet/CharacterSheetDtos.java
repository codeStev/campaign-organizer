package com.campaignorganizer.sheet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CharacterSheetDtos {

    private CharacterSheetDtos() {
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

        public static CharacterSheetResponse from(CharacterSheet s) {
            return new CharacterSheetResponse(s.getId(), s.getWorldId(), s.getTemplateId(),
                    s.getArticleId(), s.getCampaignId(), s.getName(), s.getValues(),
                    s.getCreatedAt(), s.getUpdatedAt());
        }
    }
}
