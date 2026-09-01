package com.campaignorganizer.campaign.adapter.encounter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EncounterWebDtos {

    private EncounterWebDtos() {
    }

    public record EncounterEntryDto(
            @NotNull UUID statblockId,
            @Min(1) @Max(20) int quantity) {
    }

    public record EncounterRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 20000) String notes,
            @NotNull List<@Valid EncounterEntryDto> entries) {
    }

    public record EncounterResponse(
            UUID id,
            UUID campaignId,
            String name,
            String notes,
            List<EncounterEntryDto> entries,
            Instant createdAt,
            Instant updatedAt) {
    }
}
