package com.campaignorganizer.campaign.adapter.session.out.persistence;

/** JSON-serialisable line stored in the session_transcripts.lines jsonb column (kind as its name). */
public record TranscriptLineJson(long offsetMs, String speaker, String kind, String text) {
}
