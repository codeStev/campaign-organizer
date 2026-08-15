package com.campaignorganizer.worldbuilding.domain.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test. */
class CalendarTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void createStampsTimestampsAndKeepsMonths() {
        Calendar c = Calendar.create(UUID.randomUUID(), UUID.randomUUID(), "Reckoning", 7,
                List.of(new Month("Frostfall", 30), new Month("Sunreach", 31)), T0);

        assertThat(c.getMonths()).hasSize(2);
        assertThat(c.getMonths().get(1).days()).isEqualTo(31);
        assertThat(c.getCreatedAt()).isEqualTo(T0);
        assertThat(c.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void updateReplacesMonthsAndBumpsUpdatedAt() {
        Calendar c = Calendar.create(UUID.randomUUID(), UUID.randomUUID(), "R", 7,
                List.of(new Month("A", 30)), T0);

        c.update("R2", 5, List.of(new Month("Onemonth", 10)), T1);

        assertThat(c.getName()).isEqualTo("R2");
        assertThat(c.getMonths()).hasSize(1);
        assertThat(c.getMonths().get(0).name()).isEqualTo("Onemonth");
        assertThat(c.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void rejectsEmptyMonths() {
        assertThatThrownBy(() ->
                Calendar.create(UUID.randomUUID(), UUID.randomUUID(), "R", 7, List.of(), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() ->
                Calendar.create(UUID.randomUUID(), UUID.randomUUID(), " ", 7, List.of(new Month("A", 1)), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void monthRejectsNonPositiveDays() {
        assertThatThrownBy(() -> new Month("A", 0)).isInstanceOf(ValidationException.class);
    }
}
