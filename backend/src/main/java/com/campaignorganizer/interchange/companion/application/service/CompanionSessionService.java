package com.campaignorganizer.interchange.companion.application.service;

import com.campaignorganizer.campaign.application.session.port.published.SessionExportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort.LineInput;
import com.campaignorganizer.interchange.companion.application.port.in.CompanionSession;
import com.campaignorganizer.interchange.companion.application.port.in.CreateCompanionSessionUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.DeleteSessionTranscriptUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.ListCompanionSessionsUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.WriteSessionSummaryUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Companion export of sessions, summaries and transcripts (ADR-0121). Every call first proves the
 * campaign belongs to the account; the campaign context then enforces what may be stored (a session
 * summary is never overwritten blindly, a transcript only ever holds story lines).
 */
@Service
public class CompanionSessionService implements ListCompanionSessionsUseCase, CreateCompanionSessionUseCase,
        WriteSessionSummaryUseCase, ReplaceSessionTranscriptUseCase, DeleteSessionTranscriptUseCase {

    private final CompanionCampaignAccess access;
    private final SessionExportPort sessions;
    private final SessionTranscriptWritePort transcripts;
    private final CompanionSessionMapper mapper;

    public CompanionSessionService(CompanionCampaignAccess access, SessionExportPort sessions,
                                   SessionTranscriptWritePort transcripts, CompanionSessionMapper mapper) {
        this.access = access;
        this.sessions = sessions;
        this.transcripts = transcripts;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanionSession> list(UUID accountId, UUID campaignId) {
        access.requireOwned(accountId, campaignId);
        return sessions.listForExport(campaignId).stream().map(mapper::toSession).toList();
    }

    @Override
    @Transactional
    public CompanionSession create(CreateCompanionSessionCommand command) {
        access.requireOwned(command.accountId(), command.campaignId());
        return mapper.toSession(sessions.createForExport(command.campaignId(), command.title(), command.date(),
                command.sessionNumber()));
    }

    @Override
    @Transactional
    public CompanionSession write(WriteSessionSummaryCommand command) {
        access.requireOwned(command.accountId(), command.campaignId());
        return mapper.toSession(sessions.writeSummary(command.campaignId(), command.sessionId(), command.summary(),
                command.expectedUpdatedAt()));
    }

    @Override
    @Transactional
    public TranscriptReceipt replace(ReplaceSessionTranscriptCommand command) {
        access.requireOwned(command.accountId(), command.campaignId());
        List<LineInput> lines = command.lines().stream()
                .map(l -> new LineInput(l.offsetMs(), l.speaker(), l.kind(), l.text()))
                .toList();
        return mapper.toReceipt(transcripts.replace(command.campaignId(), command.sessionId(), lines));
    }

    @Override
    @Transactional
    public void delete(UUID accountId, UUID campaignId, UUID sessionId) {
        access.requireOwned(accountId, campaignId);
        transcripts.delete(campaignId, sessionId);
    }
}
