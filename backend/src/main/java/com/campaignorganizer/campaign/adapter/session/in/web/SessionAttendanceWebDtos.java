package com.campaignorganizer.campaign.adapter.session.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Web request/response models for session attendance (ADR-0091). */
public final class SessionAttendanceWebDtos {

    private SessionAttendanceWebDtos() {
    }

    public record AttendanceEntryRequest(@NotNull UUID playerId, boolean present, UUID characterId) {
    }

    public record AttendanceRequest(@NotNull @Valid List<AttendanceEntryRequest> entries) {
    }

    public record AttendanceEntryResponse(UUID playerId, String name, boolean guest, boolean present,
                                          UUID characterId, String characterName) {
    }
}
