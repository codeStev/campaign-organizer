package com.campaignorganizer.calendar;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CalendarDtos {

    private CalendarDtos() {
    }

    public record CalendarMonthInput(
            @NotBlank @Size(max = 100) String name,
            @Positive int days) {
    }

    public record CalendarRequest(
            @NotBlank @Size(max = 200) String name,
            @Positive Integer daysPerWeek,
            @NotEmpty @Valid List<CalendarMonthInput> months) {
    }

    public record CalendarMonthResponse(String name, int days) {

        public static CalendarMonthResponse from(CalendarMonth m) {
            return new CalendarMonthResponse(m.getName(), m.getDays());
        }
    }

    public record CalendarResponse(
            UUID id,
            UUID worldId,
            String name,
            Integer daysPerWeek,
            List<CalendarMonthResponse> months,
            Instant createdAt,
            Instant updatedAt) {

        public static CalendarResponse from(Calendar c, List<CalendarMonth> months) {
            return new CalendarResponse(c.getId(), c.getWorldId(), c.getName(), c.getDaysPerWeek(),
                    months.stream().map(CalendarMonthResponse::from).toList(),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
