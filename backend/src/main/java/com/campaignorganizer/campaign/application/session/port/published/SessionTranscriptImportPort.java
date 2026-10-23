package com.campaignorganizer.campaign.application.session.port.published;

/** Published import for backup/restore (ADR-0061): saves a transcript verbatim. */
public interface SessionTranscriptImportPort {

    SessionTranscriptView importTranscript(SessionTranscriptView view);
}
