package com.campaignorganizer.campaign.adapter.loosethread.in.web;

import com.campaignorganizer.campaign.domain.loosethread.LooseThreadStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class LooseThreadWebDtos {

    private LooseThreadWebDtos() {
    }

    public record LooseThreadRequest(
            @NotBlank @Size(max = 2000) String text,
            LooseThreadStatus status) {
    }

    public record LooseThreadResponse(
            UUID id,
            UUID sessionId,
            UUID campaignId,
            String text,
            LooseThreadStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }
}
