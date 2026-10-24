package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.session.port.in.DeleteSessionTranscriptUseCase;
import com.campaignorganizer.campaign.application.session.port.in.GetSessionTranscriptUseCase;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionTranscriptRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptView;
import com.campaignorganizer.campaign.application.session.port.published.SessionTranscriptWritePort;
import com.campaignorganizer.campaign.domain.session.SessionTranscript;
import com.campaignorganizer.campaign.domain.session.TranscriptLine;
import com.campaignorganizer.campaign.domain.session.TranscriptLineKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session transcript use cases (ADR-0121); also implements the published query, import and write
 * ports. A transcript only ever holds in-character and narration lines (see
 * {@link TranscriptLineKind}), so this service cannot be made to store anything else.
 */
@Service
public class SessionTranscriptService implements GetSessionTranscriptUseCase, DeleteSessionTranscriptUseCase,
        SessionTranscriptQueryPort, SessionTranscriptImportPort, SessionTranscriptWritePort {

    private final SessionTranscriptRepositoryPort transcripts;
    private final SessionRepositoryPort sessions;
    private final CampaignExistsPort campaigns;
    private final SessionTranscriptViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public SessionTranscriptService(SessionTranscriptRepositoryPort transcripts, SessionRepositoryPort sessions,
                                    CampaignExistsPort campaigns, SessionTranscriptViewMapper viewMapper,
                                    IdGenerator ids, Clock clock) {
        this.transcripts = transcripts;
        this.sessions = sessions;
        this.campaigns = campaigns;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public SessionTranscriptView get(UUID worldId, UUID campaignId, UUID sessionId) {
        requireSession(worldId, campaignId, sessionId);
        return transcripts.findBySession(sessionId).map(viewMapper::toView)
                .orElseGet(() -> new SessionTranscriptView(null, sessionId, List.of(), null, null));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID sessionId) {
        requireSession(worldId, campaignId, sessionId);
        transcripts.findBySession(sessionId).ifPresent(transcripts::delete);
    }

    // --- published write port (companion export, ADR-0121) ---

    @Override
    @Transactional
    public SessionTranscriptView replace(UUID campaignId, UUID sessionId, List<LineInput> inputs) {
        requireSessionInCampaign(campaignId, sessionId);
        List<TranscriptLine> lines = (inputs == null ? List.<LineInput>of() : inputs).stream()
                .map(in -> new TranscriptLine(in.offsetMs(), in.speaker(), TranscriptLineKind.parse(in.kind()),
                        in.text()))
                .toList();
        SessionTranscript saved = transcripts.findBySession(sessionId)
                .map(existing -> {
                    existing.replaceLines(lines, clock.instant());
                    return existing;
                })
                .orElseGet(() -> SessionTranscript.create(ids.newId(), sessionId, lines, clock.instant()));
        return viewMapper.toView(transcripts.save(saved));
    }

    @Override
    @Transactional
    public void delete(UUID campaignId, UUID sessionId) {
        requireSessionInCampaign(campaignId, sessionId);
        transcripts.findBySession(sessionId).ifPresent(transcripts::delete);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionTranscriptView> findBySession(UUID sessionId) {
        return transcripts.findBySession(sessionId).map(viewMapper::toView);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public SessionTranscriptView importTranscript(SessionTranscriptView view) {
        List<TranscriptLine> lines = view.lines().stream()
                .map(l -> new TranscriptLine(l.offsetMs(), l.speaker(), TranscriptLineKind.parse(l.kind()), l.text()))
                .toList();
        SessionTranscript transcript = SessionTranscript.reconstitute(view.id(), view.sessionId(), lines,
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(transcripts.save(transcript));
    }

    private void requireSession(UUID worldId, UUID campaignId, UUID sessionId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
        requireSessionInCampaign(campaignId, sessionId);
    }

    private void requireSessionInCampaign(UUID campaignId, UUID sessionId) {
        sessions.findByIdAndCampaign(sessionId, campaignId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
    }
}
