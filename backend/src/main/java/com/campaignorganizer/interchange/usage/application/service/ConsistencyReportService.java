package com.campaignorganizer.interchange.usage.application.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.interchange.usage.application.port.in.ConsistencyDtos.ArticleIssue;
import com.campaignorganizer.interchange.usage.application.port.in.ConsistencyDtos.BrokenLink;
import com.campaignorganizer.interchange.usage.application.port.in.ConsistencyDtos.ConsistencyReport;
import com.campaignorganizer.interchange.usage.application.port.in.GetConsistencyReportUseCase;
import com.campaignorganizer.interchange.usage.application.port.published.UsageQueryPort;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableEntryView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * World-wide lint over the same reference machinery the usage panel uses
 * (FR-43): broken {@code [[wiki-links]]} across every body that goes through
 * the wiki pipeline, articles nothing links to, and articles no campaign
 * reaches. Orphanhood deliberately considers only article/beat/table/deck
 * inbound references — pins, timeline events and relationships are structural
 * metadata, not prose someone would look up.
 */
@Service
public class ConsistencyReportService implements GetConsistencyReportUseCase {

    private final WorldQueryPort worlds;
    private final ArticleQueryPort articles;
    private final ArticleRenderPort renderer;
    private final CampaignQueryPort campaigns;
    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;
    private final RollTableQueryPort rollTables;
    private final CardDeckQueryPort cardDecks;
    private final UsageQueryPort campaignUsage;

    public ConsistencyReportService(WorldQueryPort worlds, ArticleQueryPort articles,
                                    ArticleRenderPort renderer, CampaignQueryPort campaigns,
                                    ArcQueryPort arcs, ArcBeatQueryPort beats,
                                    RollTableQueryPort rollTables, CardDeckQueryPort cardDecks,
                                    UsageQueryPort campaignUsage) {
        this.worlds = worlds;
        this.articles = articles;
        this.renderer = renderer;
        this.campaigns = campaigns;
        this.arcs = arcs;
        this.beats = beats;
        this.cardDecks = cardDecks;
        this.rollTables = rollTables;
        this.campaignUsage = campaignUsage;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsistencyReport report(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
        List<ArticleView> worldArticles = articles.findByWorld(worldId);
        List<LinkSource> sources = collectSources(worldId, worldArticles);

        // One shared resolution pass: whatever resolveRefs cannot map is broken.
        Set<String> allTargets = new HashSet<>();
        for (LinkSource s : sources) {
            allTargets.addAll(renderer.linkTargets(s.body()));
        }
        Map<String, UUID> resolved = articles.resolveRefs(worldId, allTargets);

        return new ConsistencyReport(
                findBrokenLinks(sources, resolved),
                findOrphans(worldArticles, sources, resolved),
                findNotInAnyCampaign(worldId, worldArticles));
    }

    /** Every body the wiki pipeline renders, as a labelled link source. */
    private List<LinkSource> collectSources(UUID worldId, List<ArticleView> worldArticles) {
        List<LinkSource> sources = new ArrayList<>();
        for (ArticleView a : worldArticles) {
            sources.add(new LinkSource("ARTICLE", a.id(), a.title(), a.body()));
        }

        Map<UUID, String> arcTitles = new LinkedHashMap<>();
        List<ArcBeatView> worldBeats = new ArrayList<>();
        for (CampaignView c : campaigns.findByWorld(worldId)) {
            for (ArcView arc : arcs.findByCampaign(c.id())) {
                arcTitles.put(arc.id(), arc.title());
                worldBeats.addAll(beats.findByArc(arc.id()));
            }
        }
        for (ArcBeatView b : worldBeats) {
            String label = "Beat: " + b.title() + " — " + arcTitles.getOrDefault(b.arcId(), "arc");
            sources.add(new LinkSource("BEAT", b.id(), label, b.body(), b.articleIds()));
        }

        for (RollTableView t : rollTables.findByWorld(worldId)) {
            for (RollTableEntryView e : t.entries()) {
                sources.add(new LinkSource("ROLL_TABLE", t.id(),
                        "Roll table: " + t.title() + " (" + rangeLabel(e) + ")", e.body()));
            }
        }
        for (CardDeckView d : cardDecks.findByWorld(worldId)) {
            for (var c : d.cards()) {
                sources.add(new LinkSource("CARD_DECK", d.id(),
                        "Card deck: " + d.title() + " — " + c.title(), c.body()));
            }
        }
        return sources;
    }

    private static String rangeLabel(RollTableEntryView e) {
        if (e.minResult() == null && e.maxResult() == null) {
            return "catch-all";
        }
        if (e.minResult() != null && e.minResult().equals(e.maxResult())) {
            return e.minResult().toString();
        }
        return e.minResult() + "-" + e.maxResult();
    }

    /** One entry per unresolved (source, target); targets sorted so output is stable. */
    private List<BrokenLink> findBrokenLinks(List<LinkSource> sources, Map<String, UUID> resolved) {
        List<BrokenLink> broken = new ArrayList<>();
        for (LinkSource s : sources) {
            for (String target : new TreeSet<>(targetsOf(s))) {
                if (!resolved.containsKey(target)) {
                    broken.add(new BrokenLink(s.type(), s.id(), s.label(), target));
                }
            }
        }
        return broken;
    }

    /**
     * Articles with no inbound reference from any {@link LinkSource} body or
     * any beat's explicit reference list. A self-link does not rescue its own
     * article from orphanhood.
     */
    private List<ArticleIssue> findOrphans(List<ArticleView> worldArticles,
                                           List<LinkSource> sources,
                                           Map<String, UUID> resolved) {
        Set<UUID> referenced = new HashSet<>();
        for (LinkSource s : sources) {
            for (String target : targetsOf(s)) {
                UUID id = resolved.get(target);
                if (id != null && !("ARTICLE".equals(s.type()) && id.equals(s.id()))) {
                    referenced.add(id);
                }
            }
            referenced.addAll(s.explicitRefs());
        }
        return worldArticles.stream()
                .filter(a -> !referenced.contains(a.id()))
                .map(a -> new ArticleIssue(a.id(), a.title()))
                .toList();
    }

    private List<ArticleIssue> findNotInAnyCampaign(UUID worldId, List<ArticleView> worldArticles) {
        Set<UUID> inAnyCampaign = new HashSet<>();
        for (CampaignView c : campaigns.findByWorld(worldId)) {
            inAnyCampaign.addAll(campaignUsage.articleIdsUsedInCampaign(worldId, c.id()));
        }
        return worldArticles.stream()
                .filter(a -> !inAnyCampaign.contains(a.id()))
                .map(a -> new ArticleIssue(a.id(), a.title()))
                .toList();
    }

    private Set<String> targetsOf(LinkSource s) {
        return renderer.linkTargets(s.body());
    }

    /** A link-bearing body plus any explicit article references it carries. */
    private record LinkSource(String type, UUID id, String label, String body, List<UUID> explicitRefs) {
        LinkSource(String type, UUID id, String label, String body) {
            this(type, id, label, body, List.of());
        }
    }
}
