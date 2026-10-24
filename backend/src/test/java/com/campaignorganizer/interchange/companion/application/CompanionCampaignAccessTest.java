package com.campaignorganizer.interchange.companion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.interchange.companion.application.service.CompanionCampaignAccess;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The one ownership rule of the companion API (ADR-0120): only the owner's campaigns exist. */
@ExtendWith(MockitoExtension.class)
class CompanionCampaignAccessTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Mock
    private CampaignQueryPort campaigns;
    @Mock
    private WorldQueryPort worlds;

    private final UUID accountId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private CompanionCampaignAccess access;

    @BeforeEach
    void setUp() {
        access = new CompanionCampaignAccess(campaigns, worlds);
    }

    private CampaignView campaign() {
        return new CampaignView(campaignId, worldId, "Echos des Chaos", null, null, CampaignStatus.ACTIVE, null, null,
                NOW, NOW);
    }

    private WorldView world(UUID ownerId) {
        return new WorldView(worldId, "Aeron", null, Map.of(), false, ownerId, NOW, NOW);
    }

    @Test
    void theOwnerGetsTheCampaign() {
        when(campaigns.findById(campaignId)).thenReturn(Optional.of(campaign()));
        when(worlds.findById(worldId)).thenReturn(Optional.of(world(accountId)));

        assertThat(access.requireOwned(accountId, campaignId).id()).isEqualTo(campaignId);
    }

    @Test
    void anotherAccountsCampaignIsNotFound() {
        when(campaigns.findById(campaignId)).thenReturn(Optional.of(campaign()));
        when(worlds.findById(worldId)).thenReturn(Optional.of(world(UUID.randomUUID())));

        assertThatThrownBy(() -> access.requireOwned(accountId, campaignId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void anUnknownCampaignOrWorldIsNotFound() {
        when(campaigns.findById(campaignId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.requireOwned(accountId, campaignId)).isInstanceOf(NotFoundException.class);

        when(campaigns.findById(campaignId)).thenReturn(Optional.of(campaign()));
        when(worlds.findById(worldId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> access.requireOwned(accountId, campaignId)).isInstanceOf(NotFoundException.class);
    }
}
