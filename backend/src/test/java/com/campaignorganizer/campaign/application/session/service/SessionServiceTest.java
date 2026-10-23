package com.campaignorganizer.campaign.application.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.CreateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionExportView;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.domain.session.Session;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.ConflictException;
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

/** Application service unit test for sessions with mocked ports. */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepositoryPort sessions;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final SessionViewMapper viewMapper = new SessionViewMapperImpl();

    private SessionService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SessionService(sessions, campaigns, viewMapper, new SessionExportViewMapperImpl(), ids, clock);
    }

    @Test
    void createSucceeds() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SessionView view = service.create(new CreateSessionCommand(
                worldId, campaignId, "Session 1", 1, null, null, null));

        assertThat(view.title()).isEqualTo("Session 1");
    }

    @Test
    void createRejectsMissingCampaign() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateSessionCommand(
                worldId, campaignId, "Session 1", 1, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    private Session stored(Integer number, String summary) {
        return Session.create(UUID.randomUUID(), campaignId, "S" + number, number, null, summary, "secret notes",
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void listForExportShowsOnlyWhetherASummaryExistsNeverItsText() {
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(stored(1, "private recap"), stored(2, null)));

        var result = service.listForExport(campaignId);

        assertThat(result).extracting(SessionExportView::hasSummary)
                .containsExactly(true, false);
    }

    @Test
    void createForExportNumbersTheSessionAfterTheHighestOne() {
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(stored(3, null), stored(7, null)));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = service.createForExport(campaignId, "Session 2026-10-23", LocalDate.parse("2026-10-23"), null);

        assertThat(created.sessionNumber()).isEqualTo(8);
        assertThat(created.updatedAt().getNano() % 1000).isZero();
        assertThat(created.hasSummary()).isFalse();
    }

    @Test
    void createForExportStartsAtOneAndKeepsAnExplicitNumber() {
        when(sessions.findOrdered(campaignId)).thenReturn(List.of());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.createForExport(campaignId, "S", null, null).sessionNumber()).isEqualTo(1);
        assertThat(service.createForExport(campaignId, "S", null, 12).sessionNumber()).isEqualTo(12);
    }

    @Test
    void writeSummaryRefusesToOverwriteTheGmsOwnSummary() {
        Session existing = stored(1, "GM wrote this");
        when(sessions.findByIdAndCampaign(existing.getId(), campaignId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.writeSummary(campaignId, existing.getId(), "export", null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void writeSummaryUpdatesAnEmptySessionAndReturnsTheNewUpdateTime() {
        Session empty = stored(1, null);
        when(sessions.findByIdAndCampaign(empty.getId(), campaignId)).thenReturn(Optional.of(empty));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var view = service.writeSummary(campaignId, empty.getId(), "recap", null);

        assertThat(view.hasSummary()).isTrue();
        assertThat(view.updatedAt()).isEqualTo(Instant.parse("2026-03-03T00:00:00Z"));
    }
}
