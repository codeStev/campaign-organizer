package com.campaignorganizer.characters.adapter.document.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class DocumentWebDtos {

    private DocumentWebDtos() {
    }

    public record DocumentRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull UUID templateId,
            UUID campaignId,
            Map<String, Object> values) {
    }

    public record DocumentResponse(
            UUID id,
            UUID worldId,
            UUID templateId,
            UUID campaignId,
            String name,
            Map<String, Object> values,
            Instant createdAt,
            Instant updatedAt) {
    }
}
