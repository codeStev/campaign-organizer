package com.campaignorganizer.interchange.companion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.session.port.published.SessionExportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionExportView;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptView;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort.LineInput;
import com.campaignorganizer.interchange.companion.application.port.in.CompanionSession;
import com.campaignorganizer.interchange.companion.application.port.in.CreateCompanionSessionUseCase.CreateCompanionSessionCommand;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase.Line;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase.ReplaceSessionTranscriptCommand;
import com.campaignorganizer.interchange.companion.application.port.in.ReplaceSessionTranscriptUseCase.TranscriptReceipt;
import com.campaignorganizer.interchange.companion.application.port.in.WriteSessionSummaryUseCase.WriteSessionSummaryCommand;
import com.campaignorganizer.interchange.companion.application.service.CompanionCampaignAccess;
import com.campaignorganizer.interchange.companion.application.service.CompanionSessionMapperImpl;
import com.campaignorganizer.interchange.companion.application.service.CompanionSessionService;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit coverage for the companion export of sessions, summaries and transcripts (ADR-0121). */
@ExtendWith(MockitoExtension.class)
class CompanionSessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Mock
    private CompanionCampaignAccess access;
    @Mock
    private SessionExportPort sessions;
    @Mock
    private SessionTranscriptWritePort transcripts;

    private final UUID accountId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private CompanionSessionService service;

    @BeforeEach
    void setUp() {
        service = new CompanionSessionService(access, sessions, transcripts, new CompanionSessionMapperImpl());
    }

    private void denyAccess() {
        when(access.requireOwned(accountId, campaignId)).thenThrow(new NotFoundException("Campaign not found"));
    }

    private SessionExportView exportView(boolean hasSummary) {
        return new SessionExportView(sessionId, "Session 3", 3, LocalDate.parse("2026-10-23"), hasSummary, NOW);
    }

    @Test
    void listsTheCampaignsSessionsWithoutAnyText() {
        sessions(true);
        assertThat(service.list(accountId, campaignId)).containsExactly(
                new CompanionSession(sessionId, "Session 3", 3, LocalDate.parse("2026-10-23"), true, NOW));
    }

    private void sessions(boolean hasSummary) {
        when(sessions.listForExport(campaignId)).thenReturn(List.of(exportView(hasSummary)));
    }

    @Test
    void createsASessionAfterCheckingOwnership() {
        when(sessions.createForExport(campaignId, "Session 2026-10-23", LocalDate.parse("2026-10-23"), null))
                .thenReturn(exportView(false));

        CompanionSession created = service.create(new CreateCompanionSessionCommand(accountId, campaignId,
                "Session 2026-10-23", LocalDate.parse("2026-10-23"), null));

        verify(access).requireOwned(accountId, campaignId);
        assertThat(created.hasSummary()).isFalse();
    }

    @Test
    void writesTheSummaryWithTheExpectedUpdateTime() {
        when(sessions.writeSummary(campaignId, sessionId, "Recap", NOW)).thenReturn(exportView(true));

        CompanionSession written = service.write(new WriteSessionSummaryCommand(accountId, campaignId, sessionId,
                "Recap", NOW));

        assertThat(written.hasSummary()).isTrue();
    }

    @Test
    void replacesTheTranscriptAndReportsTheLineCount() {
        SessionTranscriptView stored = new SessionTranscriptView(UUID.randomUUID(), sessionId, List.of(
                new SessionTranscriptView.LineView(0, null, "NARRATION", "a"),
                new SessionTranscriptView.LineView(1, "Eckart", "IN_CHARACTER", "b")), NOW, NOW);
        when(transcripts.replace(campaignId, sessionId, List.of(new LineInput(0, null, "NARRATION", "a"),
                new LineInput(1, "Eckart", "IN_CHARACTER", "b")))).thenReturn(stored);

        TranscriptReceipt receipt = service.replace(new ReplaceSessionTranscriptCommand(accountId, campaignId,
                sessionId, List.of(new Line(0, null, "NARRATION", "a"), new Line(1, "Eckart", "IN_CHARACTER", "b"))));

        assertThat(receipt).isEqualTo(new TranscriptReceipt(2, NOW));
    }

    @Test
    void deletesTheTranscript() {
        service.delete(accountId, campaignId, sessionId);

        verify(transcripts).delete(campaignId, sessionId);
    }

    @Test
    void nothingIsTouchedForACampaignTheAccountDoesNotOwn() {
        denyAccess();

        assertThatThrownBy(() -> service.list(accountId, campaignId)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.create(new CreateCompanionSessionCommand(accountId, campaignId, "t", null,
                null))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.write(new WriteSessionSummaryCommand(accountId, campaignId, sessionId, "s",
                null))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.replace(new ReplaceSessionTranscriptCommand(accountId, campaignId, sessionId,
                List.of(new Line(0, null, "NARRATION", "a"))))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.delete(accountId, campaignId, sessionId))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(sessions, transcripts);
    }
}
