package com.campaignorganizer.interchange.calendar.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.interchange.calendar.application.port.in.ExportCampaignIcsUseCase.IcsCalendar;
import com.campaignorganizer.interchange.calendar.application.port.out.CalendarFeedRepositoryPort;
import com.campaignorganizer.interchange.calendar.domain.CalendarFeed;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignCalendarServiceTest {

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CalendarFeedRepositoryPort feeds;
    @Mock
    private CampaignQueryPort campaigns;
    @Mock
    private SessionQueryPort sessions;
    @Mock
    private IdGenerator ids;

    private CampaignCalendarService service;

    @BeforeEach
    void setUp() {
        service = new CampaignCalendarService(feeds, campaigns, sessions, ids, clock);
    }

    @Test
    void getOrCreateReturnsExistingTokenWithoutMintingANewOne() {
        UUID existingToken = UUID.randomUUID();
        when(campaigns.findByIdInWorld(campaignId, worldId)).thenReturn(Optional.of(campaign()));
        when(feeds.findByCampaignId(campaignId))
                .thenReturn(Optional.of(CalendarFeed.reconstitute(campaignId, existingToken, Instant.EPOCH)));

        UUID result = service.getOrCreateToken(worldId, campaignId);

        assertThat(result).isEqualTo(existingToken);
    }

    @Test
    void getOrCreateMintsAndPersistsATokenWhenNoneExists() {
        UUID newToken = UUID.randomUUID();
        when(campaigns.findByIdInWorld(campaignId, worldId)).thenReturn(Optional.of(campaign()));
        when(feeds.findByCampaignId(campaignId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(newToken);
        when(feeds.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID result = service.getOrCreateToken(worldId, campaignId);

        assertThat(result).isEqualTo(newToken);
    }

    @Test
    void regenerateReplacesAnExistingToken() {
        UUID oldToken = UUID.randomUUID();
        UUID newToken = UUID.randomUUID();
        when(campaigns.findByIdInWorld(campaignId, worldId)).thenReturn(Optional.of(campaign()));
        when(feeds.findByCampaignId(campaignId))
                .thenReturn(Optional.of(CalendarFeed.reconstitute(campaignId, oldToken, Instant.EPOCH)));
        when(ids.newId()).thenReturn(newToken);
        when(feeds.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID result = service.regenerateToken(worldId, campaignId);

        assertThat(result).isEqualTo(newToken).isNotEqualTo(oldToken);
    }

    @Test
    void exportForCampaignBuildsIcsFromThatCampaignsSessions() {
        when(campaigns.findByIdInWorld(campaignId, worldId)).thenReturn(Optional.of(campaign()));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(
                new SessionView(UUID.randomUUID(), campaignId, "Session 1", 1,
                        LocalDate.parse("2026-08-01"), null, null, Instant.EPOCH, Instant.EPOCH)));

        IcsCalendar result = service.exportForCampaign(worldId, campaignId);

        assertThat(result.campaignName()).isEqualTo("Chronicle");
        assertThat(result.icsText()).contains("SUMMARY:Chronicle — Session 1");
    }

    @Test
    void exportByTokenResolvesTheOwningCampaign() {
        UUID token = UUID.randomUUID();
        when(feeds.findByToken(token))
                .thenReturn(Optional.of(CalendarFeed.reconstitute(campaignId, token, Instant.EPOCH)));
        when(campaigns.findById(campaignId)).thenReturn(Optional.of(campaign()));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of());

        IcsCalendar result = service.exportByToken(token);

        assertThat(result.campaignName()).isEqualTo("Chronicle");
    }

    @Test
    void exportByUnknownTokenIsNotFound() {
        UUID token = UUID.randomUUID();
        when(feeds.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exportByToken(token)).isInstanceOf(NotFoundException.class);
    }

    private CampaignView campaign() {
        return new CampaignView(campaignId, worldId, "Chronicle", null, null, CampaignStatus.ACTIVE, null,
                null, Instant.EPOCH, Instant.EPOCH);
    }
}
