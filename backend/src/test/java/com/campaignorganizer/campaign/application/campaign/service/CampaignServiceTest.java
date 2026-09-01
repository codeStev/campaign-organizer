package com.campaignorganizer.campaign.application.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.campaign.port.in.CampaignCommands.CreateCampaignCommand;
import com.campaignorganizer.campaign.application.campaign.port.out.CampaignRepositoryPort;
import com.campaignorganizer.campaign.application.campaign.port.out.WorldExistsPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
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

/** Application service unit test for campaigns with mocked ports. */
@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepositoryPort campaigns;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final CampaignViewMapper viewMapper = new CampaignViewMapperImpl();

    private CampaignService service;

    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CampaignService(campaigns, worlds, viewMapper, ids, clock);
    }

    @Test
    void createReturnsViewWithGeneratedId() {
        UUID id = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(id);
        when(campaigns.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignView view = service.create(
                new CreateCampaignCommand(worldId, "Rise", "desc", "notes", CampaignStatus.ACTIVE));

        assertThat(view.id()).isEqualTo(id);
        assertThat(view.name()).isEqualTo("Rise");
        assertThat(view.status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void createDefaultsStatusToPlannedWhenNull() {
        UUID id = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(id);
        when(campaigns.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CampaignView view = service.create(new CreateCampaignCommand(worldId, "Rise", null, null, null));

        assertThat(view.status()).isEqualTo(CampaignStatus.PLANNED);
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateCampaignCommand(worldId, "Rise", null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }
}
