package com.campaignorganizer.campaign.adapter.session.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class SessionWebDtos {

    private SessionWebDtos() {
    }

    public record SessionRequest(
            @NotBlank @Size(max = 200) String title,
            Integer sessionNumber,
            LocalDate date,
            @Size(max = 20000) String summary,
            @Size(max = 20000) String notes) {
    }

    public record SessionResponse(
            UUID id,
            UUID campaignId,
            String title,
            Integer sessionNumber,
            LocalDate date,
            String summary,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
    }
}
