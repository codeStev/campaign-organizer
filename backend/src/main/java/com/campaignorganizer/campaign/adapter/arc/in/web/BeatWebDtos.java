package com.campaignorganizer.campaign.adapter.arc.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BeatWebDtos {

    private BeatWebDtos() {
    }

    public record BeatRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 20000) String body,
            Boolean done,
            List<UUID> articleIds,
            List<UUID> statblockIds,
            List<UUID> tableIds,
            List<UUID> deckIds,
            UUID sessionId,
            Integer position) {

        public boolean doneOrDefault() {
            return done != null && done;
        }

        public List<UUID> articleIdsOrEmpty() {
            return articleIds == null ? List.of() : articleIds.stream().distinct().toList();
        }

        public List<UUID> statblockIdsOrEmpty() {
            return statblockIds == null ? List.of() : statblockIds.stream().distinct().toList();
        }

        public List<UUID> tableIdsOrEmpty() {
            return tableIds == null ? List.of() : tableIds.stream().distinct().toList();
        }

        public List<UUID> deckIdsOrEmpty() {
            return deckIds == null ? List.of() : deckIds.stream().distinct().toList();
        }
    }

    public record BeatResponse(
            UUID id,
            UUID arcId,
            String title,
            String body,
            boolean done,
            List<UUID> articleIds,
            List<UUID> statblockIds,
            List<UUID> tableIds,
            List<UUID> deckIds,
            UUID sessionId,
            int position,
            Instant createdAt,
            Instant updatedAt) {
    }
}
