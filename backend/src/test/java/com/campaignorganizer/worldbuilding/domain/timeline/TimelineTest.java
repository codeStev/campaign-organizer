package com.campaignorganizer.worldbuilding.domain.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the timeline aggregates. */
class TimelineTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void timelineUpdateBumpsUpdatedAt() {
        Timeline t = Timeline.create(UUID.randomUUID(), UUID.randomUUID(), "Ages", "desc", null, T0);
        t.update("Ages II", "d2", UUID.randomUUID(), T1);

        assertThat(t.getName()).isEqualTo("Ages II");
        assertThat(t.getCreatedAt()).isEqualTo(T0);
        assertThat(t.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void timelineRejectsBlankName() {
        assertThatThrownBy(() ->
                Timeline.create(UUID.randomUUID(), UUID.randomUUID(), " ", null, null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void eventRejectsBlankTitle() {
        assertThatThrownBy(() -> TimelineEvent.create(UUID.randomUUID(), UUID.randomUUID(), null,
                "  ", null, 100, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}
