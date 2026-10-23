package com.campaignorganizer.interchange.companion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.interchange.companion.application.port.in.GetCampaignGlossaryUseCase.CampaignGlossary;
import com.campaignorganizer.interchange.companion.application.port.in.ListCompanionCampaignsUseCase.CompanionCampaign;
import com.campaignorganizer.interchange.companion.application.service.CompanionCampaignAccess;
import com.campaignorganizer.interchange.companion.application.service.CompanionService;
import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryTerm;
import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryTermKind;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleAliasQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit coverage for the companion use cases (ADR-0120): selection, scoping and ownership. */
@ExtendWith(MockitoExtension.class)
class CompanionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Mock
    private WorldQueryPort worlds;
    @Mock
    private CampaignQueryPort campaigns;
    @Mock
    private CompanionCampaignAccess access;
    @Mock
    private ArcQueryPort arcs;
    @Mock
    private ArcBeatQueryPort beats;
    @Mock
    private ArticleQueryPort articles;
    @Mock
    private ArticleAliasQueryPort aliases;
    @Mock
    private CharacterSheetQueryPort sheets;
    @Mock
    private StatblockQueryPort statblocks;

    private final UUID accountId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID arcId = UUID.randomUUID();
    private CompanionService service;

    @BeforeEach
    void setUp() {
        service = new CompanionService(worlds, campaigns, access, arcs, beats, articles, aliases, sheets, statblocks);
    }

    private WorldView world(UUID ownerId) {
        return new WorldView(worldId, "Aeron", null, Map.of(), false, ownerId, NOW, NOW);
    }

    private CampaignView campaign() {
        return new CampaignView(campaignId, worldId, "Echos des Chaos", null, null, CampaignStatus.ACTIVE, null,
                null, NOW, NOW);
    }

    private ArticleSummaryView article(UUID id, String title, String template) {
        return new ArticleSummaryView(id, worldId, null, null, title, title.toLowerCase(), template, NOW, NOW);
    }

    private StatblockView statblock(UUID id, UUID articleId, UUID statblockCampaignId, String name) {
        return new StatblockView(id, worldId, null, articleId, statblockCampaignId, null, null, name, Map.of(), null,
                NOW, NOW);
    }

    private void givenOwnedCampaign() {
        when(access.requireOwned(accountId, campaignId)).thenReturn(campaign());
        when(arcs.findByCampaign(campaignId)).thenReturn(List.of(new ArcView(arcId, campaignId, "Arc", null, "ACTIVE",
                0, NOW, NOW)));
    }

    @Test
    void listsTheCampaignsOfEveryOwnedWorldWithTheirWorldName() {
        when(worlds.findByOwner(accountId)).thenReturn(List.of(world(accountId)));
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(campaign()));

        List<CompanionCampaign> result = service.list(accountId);

        assertThat(result).containsExactly(new CompanionCampaign(campaignId, "Echos des Chaos", CampaignStatus.ACTIVE,
                worldId, "Aeron"));
    }

    @Test
    void anAccountWithoutWorldsHasNoCampaigns() {
        when(worlds.findByOwner(accountId)).thenReturn(List.of());

        assertThat(service.list(accountId)).isEmpty();
    }

    @Test
    void buildsTheGlossaryFromBeatLinksSheetsAndStatblocksOfThisCampaignOnly() {
        givenOwnedCampaign();
        UUID eckart = UUID.randomUUID();
        UUID rabenfels = UUID.randomUUID();
        UUID genericNote = UUID.randomUUID();
        UUID sheetArticle = UUID.randomUUID();
        UUID beatStatblock = UUID.randomUUID();
        UUID ownStatblock = UUID.randomUUID();
        UUID otherCampaignStatblock = UUID.randomUUID();
        when(beats.linkedArticleIdsByArcs(List.of(arcId))).thenReturn(List.of(eckart, rabenfels, genericNote));
        when(beats.linkedStatblockIdsByArcs(List.of(arcId))).thenReturn(List.of(beatStatblock));
        when(sheets.findByWorldAndCampaign(worldId, campaignId)).thenReturn(List.of(
                new CharacterSheetView(UUID.randomUUID(), worldId, null, null, null, sheetArticle, campaignId, "Elara",
                        Map.of(), NOW, NOW)));
        when(statblocks.findByWorld(worldId)).thenReturn(List.of(
                statblock(beatStatblock, null, null, "Goblin Spearman"),
                statblock(ownStatblock, null, campaignId, "Nachtschatten"),
                statblock(otherCampaignStatblock, null, UUID.randomUUID(), "Secret Dragon")));
        when(articles.findSummariesByIdsInWorld(any(), any())).thenReturn(List.of(
                article(eckart, "Eckart", "CHARACTER"),
                article(rabenfels, "Rabenfels", "LOCATION"),
                article(genericNote, "Session notes", "GENERIC"),
                article(sheetArticle, "Elara Windrunner", "CHARACTER")));
        when(aliases.findByArticlesInWorld(any(), any())).thenReturn(Map.of(eckart, List.of("Gastwirt Eckart")));

        CampaignGlossary glossary = service.get(accountId, campaignId);

        assertThat(glossary.campaignName()).isEqualTo("Echos des Chaos");
        assertThat(glossary.terms()).containsExactly(
                new GlossaryTerm("Eckart", GlossaryTermKind.CHARACTER, List.of("Gastwirt Eckart")),
                new GlossaryTerm("Elara", GlossaryTermKind.CHARACTER, List.of()),
                new GlossaryTerm("Elara Windrunner", GlossaryTermKind.CHARACTER, List.of()),
                new GlossaryTerm("Goblin Spearman", GlossaryTermKind.CREATURE, List.of()),
                new GlossaryTerm("Nachtschatten", GlossaryTermKind.CREATURE, List.of()),
                new GlossaryTerm("Rabenfels", GlossaryTermKind.LOCATION, List.of()));
    }

    @Test
    void anEmptyCampaignYieldsAnEmptyGlossary() {
        givenOwnedCampaign();
        when(beats.linkedArticleIdsByArcs(List.of(arcId))).thenReturn(List.of());
        when(beats.linkedStatblockIdsByArcs(List.of(arcId))).thenReturn(List.of());
        when(sheets.findByWorldAndCampaign(worldId, campaignId)).thenReturn(List.of());
        when(statblocks.findByWorld(worldId)).thenReturn(List.of());
        when(articles.findSummariesByIdsInWorld(any(), any())).thenReturn(List.of());
        when(aliases.findByArticlesInWorld(any(), any())).thenReturn(Map.of());

        assertThat(service.get(accountId, campaignId).terms()).isEmpty();
    }
}
