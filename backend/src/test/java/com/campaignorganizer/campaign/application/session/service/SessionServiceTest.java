package com.campaignorganizer.campaign.application.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.CreateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.session.port.out.SessionRepositoryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        service = new SessionService(sessions, campaigns, viewMapper, ids, clock);
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
}
