package com.campaignorganizer.campaign.application.loosethread.service;

import com.campaignorganizer.campaign.application.loosethread.port.in.CreateLooseThreadUseCase;
import com.campaignorganizer.campaign.application.loosethread.port.in.DeleteLooseThreadUseCase;
import com.campaignorganizer.campaign.application.loosethread.port.in.ListLooseThreadsUseCase;
import com.campaignorganizer.campaign.application.loosethread.port.in.LooseThreadCommands.CreateLooseThreadCommand;
import com.campaignorganizer.campaign.application.loosethread.port.in.LooseThreadCommands.UpdateLooseThreadCommand;
import com.campaignorganizer.campaign.application.loosethread.port.in.UpdateLooseThreadUseCase;
import com.campaignorganizer.campaign.application.loosethread.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.loosethread.port.out.LooseThreadRepositoryPort;
import com.campaignorganizer.campaign.application.loosethread.port.out.SessionExistsPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadImportPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadQueryPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.domain.loosethread.LooseThread;
import com.campaignorganizer.campaign.domain.loosethread.LooseThreadStatus;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loose-thread use cases (ADR-0085); also implements the published query/import ports. */
@Service
public class LooseThreadService implements CreateLooseThreadUseCase, UpdateLooseThreadUseCase,
        DeleteLooseThreadUseCase, ListLooseThreadsUseCase, LooseThreadQueryPort, LooseThreadImportPort {

    private final LooseThreadRepositoryPort threads;
    private final CampaignExistsPort campaigns;
    private final SessionExistsPort sessions;
    private final LooseThreadViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public LooseThreadService(LooseThreadRepositoryPort threads, CampaignExistsPort campaigns,
                              SessionExistsPort sessions, LooseThreadViewMapper viewMapper,
                              IdGenerator ids, Clock clock) {
        this.threads = threads;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public LooseThreadView create(CreateLooseThreadCommand command) {
        requireSession(command.worldId(), command.campaignId(), command.sessionId());
        LooseThread created = LooseThread.create(ids.newId(), command.sessionId(), command.campaignId(),
                command.text(), command.status(), clock.instant());
        return viewMapper.toView(threads.save(created));
    }

    @Override
    @Transactional
    public LooseThreadView update(UpdateLooseThreadCommand command) {
        requireSession(command.worldId(), command.campaignId(), command.sessionId());
        LooseThread thread = require(command.threadId(), command.sessionId());
        thread.update(command.text(), command.status(), clock.instant());
        return viewMapper.toView(threads.save(thread));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID sessionId, UUID threadId) {
        requireSession(worldId, campaignId, sessionId);
        threads.delete(require(threadId, sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LooseThreadView> list(UUID worldId, UUID campaignId, UUID sessionId) {
        requireSession(worldId, campaignId, sessionId);
        return findBySession(sessionId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public LooseThreadView importLooseThread(LooseThreadView view) {
        LooseThread thread = LooseThread.reconstitute(view.id(), view.sessionId(), view.campaignId(),
                view.text(), LooseThreadStatus.valueOf(view.status()), view.createdAt(),
                view.updatedAt());
        return viewMapper.toView(threads.save(thread));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<LooseThreadView> findBySession(UUID sessionId) {
        return threads.findBySession(sessionId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LooseThreadView> findByCampaign(UUID campaignId) {
        return threads.findByCampaign(campaignId).stream().map(viewMapper::toView).toList();
    }

    private LooseThread require(UUID threadId, UUID sessionId) {
        return threads.findByIdAndSession(threadId, sessionId)
                .orElseThrow(() -> new NotFoundException("Loose thread not found"));
    }

    private void requireSession(UUID worldId, UUID campaignId, UUID sessionId) {
        if (!campaigns.existsInWorld(campaignId, worldId) || !sessions.existsInCampaign(sessionId, campaignId)) {
            throw new NotFoundException("Session not found");
        }
    }
}
