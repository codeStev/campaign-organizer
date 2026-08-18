package com.campaignorganizer.worldbuilding.adapter.calendar.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Web request/response models for calendars. */
public final class CalendarWebDtos {

    private CalendarWebDtos() {
    }

    public record MonthDto(@NotBlank @Size(max = 100) String name, @Positive int days) {
    }

    public record CalendarRequest(
            @NotBlank @Size(max = 200) String name,
            @Positive Integer daysPerWeek,
            @NotEmpty @Valid List<MonthDto> months) {
    }

    public record CalendarResponse(
            UUID id,
            UUID worldId,
            String name,
            Integer daysPerWeek,
            List<MonthDto> months,
            Instant createdAt,
            Instant updatedAt) {
    }
}
