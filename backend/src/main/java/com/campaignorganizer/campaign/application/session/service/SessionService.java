package com.campaignorganizer.campaign.application.session.service;

import com.campaignorganizer.campaign.application.session.port.in.CreateSessionUseCase;
import com.campaignorganizer.campaign.application.session.port.in.DeleteSessionUseCase;
import com.campaignorganizer.campaign.application.session.port.in.ListSessionsUseCase;
import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.CreateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.UpdateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.in.UpdateSessionUseCase;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.domain.session.Session;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Session use cases; also implements the published query port for consumers. */
@Service
public class SessionService implements CreateSessionUseCase, UpdateSessionUseCase, DeleteSessionUseCase,
        ListSessionsUseCase, SessionQueryPort {

    private final SessionRepositoryPort sessions;
    private final CampaignExistsPort campaigns;
    private final SessionViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public SessionService(SessionRepositoryPort sessions, CampaignExistsPort campaigns,
                          SessionViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.sessions = sessions;
        this.campaigns = campaigns;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SessionView create(CreateSessionCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        Session created = Session.create(ids.newId(), command.campaignId(), command.title(),
                command.sessionNumber(), command.date(), command.summary(), command.notes(),
                clock.instant());
        return viewMapper.toView(sessions.save(created));
    }

    @Override
    @Transactional
    public SessionView update(UpdateSessionCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        Session session = require(command.sessionId(), command.campaignId());
        session.update(command.title(), command.sessionNumber(), command.date(), command.summary(),
                command.notes(), clock.instant());
        return viewMapper.toView(sessions.save(session));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID sessionId) {
        requireCampaign(worldId, campaignId);
        sessions.delete(require(sessionId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionView> list(UUID worldId, UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return findOrdered(campaignId);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<SessionView> findOrdered(UUID campaignId) {
        return sessions.findOrdered(campaignId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SessionView> findByIdInCampaign(UUID sessionId, UUID campaignId) {
        return sessions.findByIdAndCampaign(sessionId, campaignId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInCampaign(UUID sessionId, UUID campaignId) {
        return sessions.findByIdAndCampaign(sessionId, campaignId).isPresent();
    }

    private Session require(UUID sessionId, UUID campaignId) {
        return sessions.findByIdAndCampaign(sessionId, campaignId)
                .orElseThrow(() -> new NotFoundException("Session not found"));
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
    }
}
