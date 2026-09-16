package com.campaignorganizer.interchange.calendar.application.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsByTokenUseCase;
import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsUseCase;
import com.campaignorganizer.interchange.calendar.application.port.in.GetOrCreateCalendarFeedUseCase;
import com.campaignorganizer.interchange.calendar.application.port.in.RegenerateCalendarFeedUseCase;
import com.campaignorganizer.interchange.calendar.application.port.out.CalendarFeedRepositoryPort;
import com.campaignorganizer.interchange.calendar.domain.CalendarFeed;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Campaign calendar export use cases (ADR-0108) — pure composition over
 * the campaign context's published ports, plus this context's own
 * CalendarFeed token storage. */
@Service
public class CampaignCalendarService implements GetOrCreateCalendarFeedUseCase, RegenerateCalendarFeedUseCase,
        ExportCampaignIcsUseCase, ExportCampaignIcsByTokenUseCase {

    private final CalendarFeedRepositoryPort feeds;
    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final IdGenerator ids;
    private final Clock clock;
    private final EntityManager entityManager;

    public CampaignCalendarService(CalendarFeedRepositoryPort feeds, CampaignQueryPort campaigns,
                                   SessionQueryPort sessions, IdGenerator ids, Clock clock,
                                   EntityManager entityManager) {
        this.feeds = feeds;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.ids = ids;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public UUID getOrCreateToken(UUID worldId, UUID campaignId) {
        CampaignView campaign = requireCampaign(worldId, campaignId);
        return feeds.findByCampaignId(campaign.id())
                .map(CalendarFeed::getToken)
                .orElseGet(() -> feeds.save(CalendarFeed.create(campaign.id(), ids.newId(), clock.instant()))
                        .getToken());
    }

    @Override
    @Transactional
    public UUID regenerateToken(UUID worldId, UUID campaignId) {
        CampaignView campaign = requireCampaign(worldId, campaignId);
        UUID newToken = ids.newId();
        CalendarFeed feed = feeds.findByCampaignId(campaign.id())
                .orElseGet(() -> CalendarFeed.create(campaign.id(), newToken, clock.instant()));
        feed.regenerate(newToken);
        return feeds.save(feed).getToken();
    }

    @Override
    @Transactional(readOnly = true)
    public IcsCalendar exportForCampaign(UUID worldId, UUID campaignId) {
        CampaignView campaign = requireCampaign(worldId, campaignId);
        return buildCalendar(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public IcsCalendar exportByToken(UUID token) {
        // Backs the public, unauthenticated GET /api/calendar/{token}.ics (ADR-0108):
        // an unguessable token is the authorization proof here, same as ADR-0114's
        // first-account bootstrap — see FirstAccountOwnershipBootstrapper. Covers every
        // table this method (and buildCalendar below) touches, campaigns and sessions alike.
        entityManager.createNativeQuery("SET LOCAL ROLE app_rls_bypass").executeUpdate();
        CalendarFeed feed = feeds.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Calendar feed not found"));
        CampaignView campaign = campaigns.findById(feed.getCampaignId())
                .orElseThrow(() -> new NotFoundException("Calendar feed not found"));
        return buildCalendar(campaign);
    }

    private IcsCalendar buildCalendar(CampaignView campaign) {
        String ics = IcsCalendarBuilder.build(campaign.name(), sessions.findOrdered(campaign.id()),
                clock.instant());
        return new IcsCalendar(campaign.name(), ics);
    }

    private CampaignView requireCampaign(UUID worldId, UUID campaignId) {
        return campaigns.findByIdInWorld(campaignId, worldId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
    }
}
