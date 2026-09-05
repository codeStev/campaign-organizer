package com.campaignorganizer.interchange.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure unit test for the RFC 5545 builder (ADR-0108) — same package, since
 * IcsCalendarBuilder is intentionally package-private. */
class IcsCalendarBuilderTest {

    private static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");

    @Test
    void buildsOneAllDayEventPerDatedSession() {
        UUID sessionId = UUID.randomUUID();
        SessionView session = new SessionView(sessionId, UUID.randomUUID(), "Session 1: Arrival", 1,
                LocalDate.parse("2026-08-01"), "The party arrives.", null, Instant.EPOCH, Instant.EPOCH);

        String ics = IcsCalendarBuilder.build("Shadows over Duskharrow", List.of(session), NOW);

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n");
        assertThat(ics).contains("BEGIN:VEVENT\r\n");
        assertThat(ics).contains("UID:session-" + sessionId + "@campaign-organizer\r\n");
        assertThat(ics).contains("DTSTART;VALUE=DATE:20260801\r\n");
        assertThat(ics).contains("DTEND;VALUE=DATE:20260802\r\n");
        assertThat(ics).contains("SUMMARY:Shadows over Duskharrow — Session 1: Arrival\r\n");
        assertThat(ics).contains("DESCRIPTION:The party arrives.\r\n");
        assertThat(ics).contains("END:VEVENT\r\n");
        assertThat(ics).endsWith("END:VCALENDAR\r\n");
    }

    @Test
    void skipsSessionsWithNoDate() {
        SessionView dated = new SessionView(UUID.randomUUID(), UUID.randomUUID(), "Dated", null,
                LocalDate.parse("2026-08-01"), null, null, Instant.EPOCH, Instant.EPOCH);
        SessionView undated = new SessionView(UUID.randomUUID(), UUID.randomUUID(), "Undated", null, null,
                null, null, Instant.EPOCH, Instant.EPOCH);

        String ics = IcsCalendarBuilder.build("Campaign", List.of(dated, undated), NOW);

        assertThat(ics).contains("Dated");
        assertThat(ics).doesNotContain("Undated");
    }

    @Test
    void omitsDescriptionWhenSummaryIsBlank() {
        SessionView session = new SessionView(UUID.randomUUID(), UUID.randomUUID(), "No summary", null,
                LocalDate.parse("2026-08-01"), "   ", null, Instant.EPOCH, Instant.EPOCH);

        String ics = IcsCalendarBuilder.build("Campaign", List.of(session), NOW);

        assertThat(ics).doesNotContain("DESCRIPTION");
    }

    @Test
    void escapesCommasSemicolonsBackslashesAndNewlinesInTextValues() {
        SessionView session = new SessionView(UUID.randomUUID(), UUID.randomUUID(), "Fights, chases; a twist",
                null, LocalDate.parse("2026-08-01"), "Line one\nLine two, with a \\backslash\\ and; semicolon",
                null, Instant.EPOCH, Instant.EPOCH);

        String ics = IcsCalendarBuilder.build("Campaign", List.of(session), NOW);

        assertThat(ics).contains("SUMMARY:Campaign — Fights\\, chases\\; a twist\r\n");
        assertThat(ics).contains(
                "DESCRIPTION:Line one\\nLine two\\, with a \\\\backslash\\\\ and\\; semicolon\r\n");
    }

    @Test
    void foldsLongLinesAt75CharactersWithCrlfSpaceContinuation() {
        String longTitle = "A".repeat(100);
        SessionView session = new SessionView(UUID.randomUUID(), UUID.randomUUID(), longTitle, null,
                LocalDate.parse("2026-08-01"), null, null, Instant.EPOCH, Instant.EPOCH);

        String ics = IcsCalendarBuilder.build("Campaign", List.of(session), NOW);

        String[] lines = ics.split("\r\n");
        for (String line : lines) {
            assertThat(line.length()).isLessThanOrEqualTo(75);
        }
        // The folded SUMMARY line reassembles to the original content once the
        // CRLF + continuation-space markers are stripped back out.
        String reassembled = ics.replace("\r\n ", "");
        assertThat(reassembled).contains("SUMMARY:Campaign — " + longTitle);
    }

    @Test
    void emptySessionListStillProducesAValidEmptyCalendar() {
        String ics = IcsCalendarBuilder.build("Campaign", List.of(), NOW);

        assertThat(ics).isEqualTo("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"
                + "PRODID:-//Campaign Organizer//Session Calendar//EN\r\nCALSCALE:GREGORIAN\r\n"
                + "END:VCALENDAR\r\n");
    }
}
