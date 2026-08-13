package com.campaignorganizer.statblock;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class StatblockDtos {

    private StatblockDtos() {
    }

    public record StatblockRequest(
            @NotBlank @Size(max = 200) String name,
            UUID articleId,
            UUID campaignId,
            Map<String, Object> stats,
            @Size(max = 20000) String notes) {
    }

    public record StatblockResponse(
            UUID id,
            UUID worldId,
            UUID articleId,
            UUID campaignId,
            String name,
            Map<String, Object> stats,
            String notes,
            Instant createdAt,
            Instant updatedAt) {

        public static StatblockResponse from(Statblock s) {
            return new StatblockResponse(s.getId(), s.getWorldId(), s.getArticleId(), s.getCampaignId(),
                    s.getName(), s.getStats(), s.getNotes(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }
}
