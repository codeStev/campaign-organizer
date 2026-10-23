package com.campaignorganizer.campaign.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ConflictException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the session aggregate. */
class SessionTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        Session s = Session.create(UUID.randomUUID(), UUID.randomUUID(), "Session 1", 1,
                LocalDate.parse("2026-01-01"), "sum", "notes", T0);
        s.update("Session 1b", 2, LocalDate.parse("2026-01-08"), "sum2", "notes2", T1);

        assertThat(s.getTitle()).isEqualTo("Session 1b");
        assertThat(s.getSessionNumber()).isEqualTo(2);
        assertThat(s.getCreatedAt()).isEqualTo(T0);
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Session.create(UUID.randomUUID(), UUID.randomUUID(), " ", null,
                null, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }

    private static Session withSummary(String summary) {
        return Session.create(UUID.randomUUID(), UUID.randomUUID(), "Session 1", 1,
                LocalDate.parse("2026-01-01"), summary, "private prep notes", T0);
    }

    @Test
    void writeSummaryFillsAnEmptySummaryWithoutAnExpectedTime() {
        Session s = withSummary(null);

        s.writeSummary("Recap", null, T1);

        assertThat(s.getSummary()).isEqualTo("Recap");
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
        assertThat(s.getNotes()).isEqualTo("private prep notes");
    }

    @Test
    void writeSummaryOverwritesOnlyWhenNothingChangedSinceTheExpectedTime() {
        Session s = withSummary("First export");

        s.writeSummary("Second export", T0, T1);

        assertThat(s.getSummary()).isEqualTo("Second export");
    }

    @Test
    void writeSummaryNeverOverwritesAnExistingSummaryTheCallerDidNotWrite() {
        Session s = withSummary("Written by the GM");

        assertThatThrownBy(() -> s.writeSummary("Export", null, T1)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> s.writeSummary("Export", T1, T1)).isInstanceOf(ConflictException.class);
        assertThat(s.getSummary()).isEqualTo("Written by the GM");
        assertThat(s.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void writeSummaryTreatsABlankSummaryAsEmpty() {
        Session s = withSummary("   ");

        s.writeSummary("Recap", null, T1);

        assertThat(s.hasSummary()).isTrue();
    }

    @Test
    void writeSummaryComparesTheExpectedTimeAtMicrosecondPrecision() {
        Session s = Session.reconstitute(UUID.randomUUID(), UUID.randomUUID(), "S", 1, null, "old", null, T0,
                Instant.parse("2026-01-01T00:00:00.123456Z"));

        s.writeSummary("new", Instant.parse("2026-01-01T00:00:00.123456789Z"), T1);

        assertThat(s.getSummary()).isEqualTo("new");
    }

    @Test
    void writeSummaryRejectsBlankAndOverlongSummaries() {
        Session s = withSummary(null);

        assertThatThrownBy(() -> s.writeSummary(" ", null, T1)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> s.writeSummary("x".repeat(Session.MAX_TEXT_LENGTH + 1), null, T1))
                .isInstanceOf(ValidationException.class);
    }
}
