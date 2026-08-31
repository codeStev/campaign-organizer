package com.campaignorganizer.characters.application.statblock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.CreateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.out.ArticleExistsPort;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignExistsPort;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignStatblockRefPort;
import com.campaignorganizer.characters.application.statblock.port.out.StatblockRepositoryPort;
import com.campaignorganizer.characters.application.statblock.port.out.StatblockTagLookupPort;
import com.campaignorganizer.characters.application.statblock.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.domain.statblock.Statblock;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for statblocks with mocked ports. */
@ExtendWith(MockitoExtension.class)
class StatblockServiceTest {

    @Mock
    private StatblockRepositoryPort statblocks;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private ArticleExistsPort articles;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private FieldTemplateQueryPort templates;
    @Mock
    private CampaignStatblockRefPort campaignRefs;
    @Mock
    private StatblockTagLookupPort tagLookup;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final StatblockViewMapper viewMapper = new StatblockViewMapperImpl();

    private StatblockService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StatblockService(statblocks, worlds, articles, campaigns, templates, campaignRefs,
                tagLookup, viewMapper, ids, clock);
    }

    private Statblock statblock(UUID id, UUID campaign) {
        return Statblock.create(id, worldId, null, campaign, null, "SB", null, null, clock.instant());
    }

    private FieldTemplateView templateView(TemplateKind kind) {
        return new FieldTemplateView(templateId, worldId, "Template", kind, null, List.of(),
                Instant.now(), Instant.now());
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateStatblockCommand(
                worldId, null, null, null, "SB", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignCampaign() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateStatblockCommand(
                worldId, null, campaignId, null, "SB", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsForeignTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.findByIdInWorld(templateId, worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateStatblockCommand(
                worldId, null, null, templateId, "SB", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsCharacterTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.findByIdInWorld(templateId, worldId))
                .thenReturn(Optional.of(templateView(TemplateKind.CHARACTER)));

        assertThatThrownBy(() -> service.create(new CreateStatblockCommand(
                worldId, null, null, templateId, "SB", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void campaignScopedListMergesScopedAndBeatReferenced() {
        UUID scoped = UUID.randomUUID();
        UUID referenced = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(statblocks.findByWorldAndCampaign(worldId, campaignId))
                .thenReturn(List.of(statblock(scoped, campaignId)));
        when(campaignRefs.statblockIdsReferencedByCampaign(campaignId))
                .thenReturn(List.of(referenced));
        when(statblocks.findAllByIds(any()))
                .thenReturn(List.of(statblock(referenced, null)));

        List<StatblockView> result = service.list(worldId, campaignId, null);

        assertThat(result).extracting(StatblockView::id).containsExactly(scoped, referenced);
    }
}
