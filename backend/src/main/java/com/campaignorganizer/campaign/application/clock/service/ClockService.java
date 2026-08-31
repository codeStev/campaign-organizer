package com.campaignorganizer.campaign.application.clock.service;

import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.CreateClockCommand;
import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.SegmentInput;
import com.campaignorganizer.campaign.application.clock.port.in.ClockCommands.UpdateClockCommand;
import com.campaignorganizer.campaign.application.clock.port.in.CreateClockUseCase;
import com.campaignorganizer.campaign.application.clock.port.in.DeleteClockUseCase;
import com.campaignorganizer.campaign.application.clock.port.in.ListClocksUseCase;
import com.campaignorganizer.campaign.application.clock.port.in.UpdateClockUseCase;
import com.campaignorganizer.campaign.application.clock.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.clock.port.out.ClockRepositoryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockImportPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockQueryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockSegmentView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.domain.clock.ClockSegment;
import com.campaignorganizer.campaign.domain.clock.GameClock;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clock use cases (ADR-0084); also implements the published query/import ports for consumers. */
@Service
public class ClockService implements CreateClockUseCase, UpdateClockUseCase, DeleteClockUseCase,
        ListClocksUseCase, ClockQueryPort, ClockImportPort {

    private final ClockRepositoryPort clocks;
    private final CampaignExistsPort campaigns;
    private final ClockViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public ClockService(ClockRepositoryPort clocks, CampaignExistsPort campaigns,
                        ClockViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.clocks = clocks;
        this.campaigns = campaigns;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ClockView create(CreateClockCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        int position = command.position() == null ? 0 : command.position();
        GameClock created = GameClock.create(ids.newId(), command.campaignId(), command.title(),
                command.description(), toDomainSegments(command.segments()), position, clock.instant());
        return viewMapper.toView(clocks.save(created));
    }

    @Override
    @Transactional
    public ClockView update(UpdateClockCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        GameClock gameClock = require(command.clockId(), command.campaignId());
        int position = command.position() == null ? gameClock.getPosition() : command.position();
        gameClock.update(command.title(), command.description(), toDomainSegments(command.segments()),
                position, clock.instant());
        return viewMapper.toView(clocks.save(gameClock));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID clockId) {
        requireCampaign(worldId, campaignId);
        clocks.delete(require(clockId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClockView> list(UUID worldId, UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return findByCampaign(campaignId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public ClockView importClock(ClockView view) {
        GameClock gameClock = GameClock.reconstitute(view.id(), view.campaignId(), view.title(),
                view.description(), toDomainSegmentsFromViews(view.segments()), view.position(),
                view.createdAt(), view.updatedAt());
        return viewMapper.toView(clocks.save(gameClock));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<ClockView> findByCampaign(UUID campaignId) {
        return clocks.findByCampaign(campaignId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClockView> findById(UUID clockId) {
        return clocks.findById(clockId).map(viewMapper::toView);
    }

    private static List<ClockSegment> toDomainSegments(List<SegmentInput> inputs) {
        if (inputs == null) {
            return List.of();
        }
        return inputs.stream()
                .map(s -> new ClockSegment(s.filled(), s.title(), s.description()))
                .toList();
    }

    private static List<ClockSegment> toDomainSegmentsFromViews(List<ClockSegmentView> views) {
        if (views == null) {
            return List.of();
        }
        return views.stream()
                .map(s -> new ClockSegment(s.filled(), s.title(), s.description()))
                .toList();
    }

    private GameClock require(UUID clockId, UUID campaignId) {
        return clocks.findByIdAndCampaign(clockId, campaignId)
                .orElseThrow(() -> new NotFoundException("Clock not found"));
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
    }
}
