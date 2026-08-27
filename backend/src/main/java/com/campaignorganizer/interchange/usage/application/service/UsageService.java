package com.campaignorganizer.interchange.usage.application.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.interchange.usage.application.port.in.GetArticleUsagesUseCase;
import com.campaignorganizer.interchange.usage.application.port.in.UsageDtos.Usage;
import com.campaignorganizer.interchange.usage.application.port.in.UsageDtos.UsageResponse;
import com.campaignorganizer.interchange.usage.application.port.published.UsageQueryPort;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipQueryPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineLookupPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates where an article is used, and which articles a campaign references
 * (ADR-0033). Composes the core contexts only through their published query
 * ports (ADR-0050); holds no core domain rules of its own.
 */
@Service
public class UsageService implements GetArticleUsagesUseCase, UsageQueryPort {

    private final ArticleQueryPort articles;
    private final ArticleRenderPort articleRenderer;
    private final ArcBeatQueryPort beats;
    private final ArcQueryPort arcs;
    private final CampaignQueryPort campaigns;
    private final MapPinQueryPort pins;
    private final MapQueryPort maps;
    private final TimelineEventQueryPort events;
    private final TimelineLookupPort timelines;
    private final RelationshipQueryPort relationships;
    private final CharacterSheetQueryPort sheets;
    private final StatblockQueryPort statblocks;
    private final RollTableQueryPort rollTables;
    private final CardDeckQueryPort cardDecks;

    public UsageService(ArticleQueryPort articles, ArticleRenderPort articleRenderer,
                        ArcBeatQueryPort beats, ArcQueryPort arcs,
                        CampaignQueryPort campaigns, MapPinQueryPort pins, MapQueryPort maps,
                        TimelineEventQueryPort events, TimelineLookupPort timelines,
                        RelationshipQueryPort relationships, CharacterSheetQueryPort sheets,
                        StatblockQueryPort statblocks, RollTableQueryPort rollTables,
                        CardDeckQueryPort cardDecks) {
        this.articles = articles;
        this.articleRenderer = articleRenderer;
        this.beats = beats;
        this.arcs = arcs;
        this.campaigns = campaigns;
        this.pins = pins;
        this.maps = maps;
        this.events = events;
        this.timelines = timelines;
        this.relationships = relationships;
        this.sheets = sheets;
        this.statblocks = statblocks;
        this.rollTables = rollTables;
        this.cardDecks = cardDecks;
    }

    @Override
    @Transactional(readOnly = true)
    public UsageResponse articleUsages(UUID worldId, UUID articleId) {
        ArticleView article = articles.findByIdInWorld(articleId, worldId)
                .orElseThrow(() -> new NotFoundException("Article not found"));
        List<Usage> out = new ArrayList<>();

        beats.findByLinkedArticle(articleId).forEach(b -> {
            ArcView arc = arcs.findById(b.arcId()).orElse(null);
            UUID campaignId = arc == null ? null : arc.campaignId();
            out.add(new Usage("BEAT",
                    "Beat: " + b.title() + (arc != null ? " — " + arc.title() : ""),
                    null, campaignId, campaignName(campaignId)));
        });

        pins.findByArticle(articleId).forEach(p -> {
            String mapName = maps.findById(p.mapId()).map(MapView::name).orElse("map");
            String label = p.label() != null ? " — " + p.label() : "";
            out.add(new Usage("MAP_PIN", "Map pin on " + mapName + label, null, null, null));
        });

        events.findByArticle(articleId).forEach(e -> {
            String tl = timelines.findById(e.timelineId()).map(TimelineView::name).orElse("timeline");
            out.add(new Usage("TIMELINE_EVENT", "Timeline event: " + e.title() + " (" + tl + ")",
                    null, null, null));
        });

        for (RelationshipView r : relationships.findTouchingArticle(worldId, articleId)) {
            UUID other = r.fromArticleId().equals(articleId) ? r.toArticleId() : r.fromArticleId();
            String otherTitle = articles.findByIdInWorld(other, worldId).map(ArticleView::title).orElse("article");
            String label = r.label() != null && !r.label().isBlank() ? r.label() : "related to";
            out.add(new Usage("RELATIONSHIP", label + " " + otherTitle, other, null, null));
        }

        sheets.findByWorldAndArticle(worldId, articleId).forEach(s ->
                out.add(new Usage("CHARACTER_SHEET", "Character sheet: " + s.name(),
                        null, s.campaignId(), campaignName(s.campaignId()))));

        statblocks.findByWorldAndArticle(worldId, articleId).forEach(s ->
                out.add(new Usage("STATBLOCK", "Statblock: " + s.name(),
                        null, s.campaignId(), campaignName(s.campaignId()))));

        // Wiki-link backlinks: other articles whose body [[links]] to this one.
        String slug = article.slug().toLowerCase(Locale.ROOT);
        String title = article.title().toLowerCase(Locale.ROOT);

        // Roll-table outcomes and deck cards can reference the article too (FR-40).
        rollTables.findByWorld(worldId).forEach(t -> t.entries().stream()
                .filter(e -> references(articleRenderer, e.body(), slug, title))
                .findAny()
                .ifPresent(e -> out.add(new Usage("ROLL_TABLE", "Roll table: " + t.title(),
                        null, null, null))));
        cardDecks.findByWorld(worldId).forEach(d -> d.cards().stream()
                .filter(c -> references(articleRenderer, c.body(), slug, title))
                .findAny()
                .ifPresent(c -> out.add(new Usage("CARD_DECK", "Card deck: " + d.title(),
                        null, null, null))));

        for (ArticleView other : articles.findByWorld(worldId)) {
            if (other.id().equals(articleId)) {
                continue;
            }
            Set<String> targets = articleRenderer.linkTargets(other.body());
            if (targets.contains(slug) || targets.contains(title)) {
                out.add(new Usage("ARTICLE_LINK", "Linked from " + other.title(),
                        other.id(), null, null));
            }
        }

        return new UsageResponse(out);
    }

    /** True when a raw body contains a wiki-link to the given slug or title. */
    private static boolean references(ArticleRenderPort renderer, String body, String slug, String title) {
        Set<String> targets = renderer.linkTargets(body);
        return targets.contains(slug) || targets.contains(title);
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> articleIdsUsedInCampaign(UUID worldId, UUID campaignId) {
        Set<UUID> ids = new HashSet<>();
        List<UUID> arcIds = arcs.findByCampaign(campaignId).stream().map(ArcView::id).toList();
        ids.addAll(beats.linkedArticleIdsByArcs(arcIds));
        for (CharacterSheetView s : sheets.findByWorldAndCampaign(worldId, campaignId)) {
            if (s.articleId() != null) {
                ids.add(s.articleId());
            }
        }
        for (StatblockView s : statblocks.findByWorldAndCampaign(worldId, campaignId)) {
            if (s.articleId() != null) {
                ids.add(s.articleId());
            }
        }
        return ids;
    }

    private String campaignName(UUID campaignId) {
        return campaignId == null ? null
                : campaigns.findById(campaignId).map(CampaignView::name).orElse(null);
    }
}
