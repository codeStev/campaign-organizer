package com.campaignorganizer.interchange.calendar.application.service;

import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds RFC 5545 iCalendar text for a campaign's dated sessions (ADR-0108).
 * Sessions map to all-day VEVENTs — Session.date has no time-of-day
 * component at all, so there's nothing to build a timed event from.
 * Undated sessions are skipped (not calendar-relevant).
 */
final class IcsCalendarBuilder {

    private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter ICS_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private IcsCalendarBuilder() {
    }

    static String build(String campaignName, List<SessionView> sessions, Instant now) {
        StringBuilder out = new StringBuilder();
        line(out, "BEGIN:VCALENDAR");
        line(out, "VERSION:2.0");
        line(out, "PRODID:-//Campaign Organizer//Session Calendar//EN");
        line(out, "CALSCALE:GREGORIAN");

        String stamp = ICS_STAMP.format(now.atZone(ZoneOffset.UTC));
        for (SessionView s : sessions) {
            if (s.date() == null) {
                continue;
            }
            line(out, "BEGIN:VEVENT");
            line(out, "UID:session-" + s.id() + "@campaign-organizer");
            line(out, "DTSTAMP:" + stamp);
            line(out, "DTSTART;VALUE=DATE:" + ICS_DATE.format(s.date()));
            line(out, "DTEND;VALUE=DATE:" + ICS_DATE.format(s.date().plusDays(1)));
            line(out, "SUMMARY:" + escape(campaignName + " — " + s.title()));
            if (s.summary() != null && !s.summary().isBlank()) {
                line(out, "DESCRIPTION:" + escape(s.summary()));
            }
            line(out, "END:VEVENT");
        }

        line(out, "END:VCALENDAR");
        return out.toString();
    }

    private static void line(StringBuilder out, String content) {
        out.append(fold(content)).append("\r\n");
    }

    /** RFC 5545 §3.1 line folding: split at 75 octets, continuation lines
     * prefixed with a single space. Uses UTF-16 code units, not true octets
     * — a reasonable simplification for a personal, mostly-ASCII app. */
    private static String fold(String content) {
        if (content.length() <= 75) {
            return content;
        }
        StringBuilder folded = new StringBuilder();
        int start = 0;
        boolean first = true;
        while (start < content.length()) {
            int limit = first ? 75 : 74; // continuation lines lose one column to the leading space
            int end = Math.min(start + limit, content.length());
            if (!first) {
                folded.append("\r\n ");
            }
            folded.append(content, start, end);
            start = end;
            first = false;
        }
        return folded.toString();
    }

    /** RFC 5545 §3.3.11 TEXT escaping. */
    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }
}
