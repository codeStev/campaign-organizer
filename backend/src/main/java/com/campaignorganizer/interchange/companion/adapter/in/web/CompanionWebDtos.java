package com.campaignorganizer.interchange.companion.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Response payloads for the companion API (mirrors docs/api/openapi.yaml). */
public final class CompanionWebDtos {

    private CompanionWebDtos() {
    }

    public record CompanionCampaignResponse(UUID id, String name, String status, UUID worldId, String worldName) {
    }

    public record GlossaryTermResponse(String name, String kind, List<String> aliases) {
    }

    public record CampaignGlossaryResponse(UUID campaignId, String campaignName, List<GlossaryTermResponse> terms) {
    }

    public record CompanionSessionResponse(UUID id, String title, Integer sessionNumber, LocalDate date,
                                           boolean hasSummary, Instant updatedAt) {
    }

    public record CreateCompanionSessionRequest(@NotBlank @Size(max = 200) String title, LocalDate date,
                                                Integer sessionNumber) {
    }

    public record WriteSessionSummaryRequest(@NotBlank @Size(max = 20000) String summary,
                                             Instant expectedUpdatedAt) {
    }

    public record TranscriptLineRequest(@Min(0) long offsetMs, @Size(max = 100) String speaker,
                                        @NotBlank String kind, @NotBlank @Size(max = 2000) String text) {
    }

    public record ReplaceTranscriptRequest(
            @NotEmpty @Size(max = 20000) List<@NotNull @Valid TranscriptLineRequest> lines) {
    }

    public record TranscriptReceiptResponse(int lineCount, Instant updatedAt) {
    }
}
