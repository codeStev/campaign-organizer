package com.campaignorganizer.interchange.companion.application.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.interchange.companion.application.port.in.GetCampaignGlossaryUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.ListCompanionCampaignsUseCase;
import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryBuilder;
import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryTermKind;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleAliasQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Companion API use cases (ADR-0120): composes the worlds, campaign, article, sheet and statblock
 * contexts through their published ports. Ownership is checked here against the world's owner, not
 * left to row-level security alone.
 */
@Service
public class CompanionService implements ListCompanionCampaignsUseCase, GetCampaignGlossaryUseCase {

    private final WorldQueryPort worlds;
    private final CampaignQueryPort campaigns;
    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;
    private final ArticleQueryPort articles;
    private final ArticleAliasQueryPort aliases;
    private final CharacterSheetQueryPort sheets;
    private final StatblockQueryPort statblocks;

    public CompanionService(WorldQueryPort worlds, CampaignQueryPort campaigns, ArcQueryPort arcs,
                            ArcBeatQueryPort beats, ArticleQueryPort articles, ArticleAliasQueryPort aliases,
                            CharacterSheetQueryPort sheets, StatblockQueryPort statblocks) {
        this.worlds = worlds;
        this.campaigns = campaigns;
        this.arcs = arcs;
        this.beats = beats;
        this.articles = articles;
        this.aliases = aliases;
        this.sheets = sheets;
        this.statblocks = statblocks;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanionCampaign> list(UUID accountId) {
        return worlds.findByOwner(accountId).stream()
                .flatMap(world -> campaigns.findByWorld(world.id()).stream()
                        .map(campaign -> new CompanionCampaign(campaign.id(), campaign.name(), campaign.status(),
                                world.id(), world.name())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignGlossary get(UUID accountId, UUID campaignId) {
        CampaignView campaign = campaigns.findById(campaignId).orElseThrow(this::campaignNotFound);
        WorldView world = worlds.findById(campaign.worldId())
                .filter(w -> accountId.equals(w.ownerId()))
                .orElseThrow(this::campaignNotFound);
        UUID worldId = world.id();

        List<UUID> arcIds = arcs.findByCampaign(campaignId).stream().map(ArcView::id).toList();
        Set<UUID> beatStatblockIds = new LinkedHashSet<>(beats.linkedStatblockIdsByArcs(arcIds));
        List<CharacterSheetView> campaignSheets = sheets.findByWorldAndCampaign(worldId, campaignId);
        List<StatblockView> campaignStatblocks = statblocks.findByWorld(worldId).stream()
                .filter(s -> campaignId.equals(s.campaignId()) || beatStatblockIds.contains(s.id()))
                .toList();

        Set<UUID> articleIds = new LinkedHashSet<>(beats.linkedArticleIdsByArcs(arcIds));
        campaignSheets.stream().map(CharacterSheetView::articleId).filter(Objects::nonNull).forEach(articleIds::add);
        campaignStatblocks.stream().map(StatblockView::articleId).filter(Objects::nonNull).forEach(articleIds::add);

        GlossaryBuilder glossary = new GlossaryBuilder();
        Map<UUID, List<String>> aliasesByArticle = aliases.findByArticlesInWorld(worldId, articleIds);
        for (ArticleSummaryView article : articles.findSummariesByIdsInWorld(worldId, articleIds)) {
            GlossaryTermKind.fromArticleTemplate(article.template()).ifPresent(kind ->
                    glossary.add(article.title(), kind, aliasesByArticle.getOrDefault(article.id(), List.of())));
        }
        campaignSheets.forEach(sheet -> glossary.add(sheet.name(), GlossaryTermKind.CHARACTER, List.of()));
        campaignStatblocks.forEach(statblock -> glossary.add(statblock.name(), GlossaryTermKind.CREATURE, List.of()));

        return new CampaignGlossary(campaignId, campaign.name(), glossary.build());
    }

    private NotFoundException campaignNotFound() {
        return new NotFoundException("Campaign not found");
    }
}
