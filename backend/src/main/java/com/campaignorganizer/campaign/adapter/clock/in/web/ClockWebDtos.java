package com.campaignorganizer.campaign.adapter.clock.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClockWebDtos {

    private ClockWebDtos() {
    }

    public record ClockSegmentDto(
            boolean filled,
            @Size(max = 200) String title,
            @Size(max = 2000) String description) {
    }

    public record ClockRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            @NotNull List<@Valid ClockSegmentDto> segments,
            Integer position) {
    }

    public record ClockResponse(
            UUID id,
            UUID campaignId,
            String title,
            String description,
            List<ClockSegmentDto> segments,
            int position,
            Instant createdAt,
            Instant updatedAt) {
    }
}
