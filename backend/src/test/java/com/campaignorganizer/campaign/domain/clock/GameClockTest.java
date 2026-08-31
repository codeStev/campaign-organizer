package com.campaignorganizer.campaign.domain.clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the clock aggregate (ADR-0084). */
class GameClockTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void clockUpdateBumpsUpdatedAt() {
        GameClock c = GameClock.create(UUID.randomUUID(), UUID.randomUUID(), "Doom", "desc",
                List.of(new ClockSegment(false, null, null)), 1, T0);
        c.update("Doom!", "desc2", List.of(new ClockSegment(true, "Alarm", null)), 2, T1);

        assertThat(c.getTitle()).isEqualTo("Doom!");
        assertThat(c.getSegments()).containsExactly(new ClockSegment(true, "Alarm", null));
        assertThat(c.getPosition()).isEqualTo(2);
        assertThat(c.getCreatedAt()).isEqualTo(T0);
        assertThat(c.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void clockNullSegmentsBecomeEmpty() {
        GameClock c = GameClock.create(UUID.randomUUID(), UUID.randomUUID(), "Doom", null, null, 0, T0);
        assertThat(c.getSegments()).isEmpty();
    }

    @Test
    void clockRejectsBlankTitle() {
        assertThatThrownBy(() ->
                GameClock.create(UUID.randomUUID(), UUID.randomUUID(), " ", null, List.of(), 0, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void segmentsAreIndividuallyAddressable() {
        List<ClockSegment> segments = List.of(
                new ClockSegment(true, null, null),
                new ClockSegment(false, "Guards alerted", "The watch sounds the horn"),
                new ClockSegment(true, null, null));
        GameClock c = GameClock.create(UUID.randomUUID(), UUID.randomUUID(), "Doom", null, segments, 0, T0);

        assertThat(c.getSegments()).hasSize(3);
        assertThat(c.getSegments().get(1).title()).isEqualTo("Guards alerted");
        assertThat(c.getSegments().get(1).filled()).isFalse();
    }
}
