package com.campaignorganizer.usage;

import com.campaignorganizer.campaign.ArcBeatRepository;
import com.campaignorganizer.campaign.ArcRepository;
import com.campaignorganizer.campaign.Campaign;
import com.campaignorganizer.campaign.CampaignRepository;
import com.campaignorganizer.map.MapPinRepository;
import com.campaignorganizer.map.WorldMap;
import com.campaignorganizer.map.WorldMapRepository;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipQueryPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import com.campaignorganizer.sheet.CharacterSheet;
import com.campaignorganizer.sheet.CharacterSheetRepository;
import com.campaignorganizer.statblock.Statblock;
import com.campaignorganizer.statblock.StatblockRepository;
import com.campaignorganizer.timeline.Timeline;
import com.campaignorganizer.timeline.TimelineRepository;
import com.campaignorganizer.timeline.TimelineEventRepository;
import com.campaignorganizer.usage.UsageDtos.Usage;
import com.campaignorganizer.usage.UsageDtos.UsageResponse;
import com.campaignorganizer.wiki.Article;
import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.wiki.AutoLinker;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Aggregates where an article is used, and which articles a campaign references (ADR-0033). */
@Service
public class UsageService {

    private final ArticleRepository articles;
    private final ArcBeatRepository beats;
    private final ArcRepository arcs;
    private final CampaignRepository campaigns;
    private final MapPinRepository pins;
    private final WorldMapRepository maps;
    private final TimelineEventRepository events;
    private final TimelineRepository timelines;
    private final RelationshipQueryPort relationships;
    private final CharacterSheetRepository sheets;
    private final StatblockRepository statblocks;

    public UsageService(ArticleRepository articles, ArcBeatRepository beats, ArcRepository arcs,
                        CampaignRepository campaigns, MapPinRepository pins, WorldMapRepository maps,
                        TimelineEventRepository events, TimelineRepository timelines,
                        RelationshipQueryPort relationships, CharacterSheetRepository sheets,
                        StatblockRepository statblocks) {
        this.articles = articles;
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
    }

    public UsageResponse articleUsages(UUID worldId, UUID articleId) {
        Article article = articles.findByIdAndWorldId(articleId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        List<Usage> out = new ArrayList<>();

        beats.findByLinkedArticleId(articleId).forEach(b -> {
            var arc = arcs.findById(b.getArcId()).orElse(null);
            UUID campaignId = arc == null ? null : arc.getCampaignId();
            out.add(new Usage("BEAT",
                    "Beat: " + b.getTitle() + (arc != null ? " — " + arc.getTitle() : ""),
                    null, campaignId, campaignName(campaignId)));
        });

        pins.findByArticleId(articleId).forEach(p -> {
            String mapName = maps.findById(p.getMapId()).map(WorldMap::getName).orElse("map");
            String label = p.getLabel() != null ? " — " + p.getLabel() : "";
            out.add(new Usage("MAP_PIN", "Map pin on " + mapName + label, null, null, null));
        });

        events.findByArticleId(articleId).forEach(e -> {
            String tl = timelines.findById(e.getTimelineId()).map(Timeline::getName).orElse("timeline");
            out.add(new Usage("TIMELINE_EVENT", "Timeline event: " + e.getTitle() + " (" + tl + ")",
                    null, null, null));
        });

        for (RelationshipView r : relationships.findTouchingArticle(worldId, articleId)) {
            UUID other = r.fromArticleId().equals(articleId) ? r.toArticleId() : r.fromArticleId();
            String otherTitle = articles.findById(other).map(Article::getTitle).orElse("article");
            String label = r.label() != null && !r.label().isBlank() ? r.label() : "related to";
            out.add(new Usage("RELATIONSHIP", label + " " + otherTitle, other, null, null));
        }

        sheets.findByWorldIdAndArticleId(worldId, articleId).forEach(s ->
                out.add(new Usage("CHARACTER_SHEET", "Character sheet: " + s.getName(),
                        null, s.getCampaignId(), campaignName(s.getCampaignId()))));

        statblocks.findByWorldIdAndArticleId(worldId, articleId).forEach(s ->
                out.add(new Usage("STATBLOCK", "Statblock: " + s.getName(),
                        null, s.getCampaignId(), campaignName(s.getCampaignId()))));

        // Wiki-link backlinks: other articles whose body [[links]] to this one.
        String slug = article.getSlug().toLowerCase(Locale.ROOT);
        String title = article.getTitle().toLowerCase(Locale.ROOT);
        for (Article other : articles.findByWorldIdOrderByCreatedAtDesc(worldId)) {
            if (other.getId().equals(articleId)) {
                continue;
            }
            Set<String> targets = AutoLinker.linkTargets(other.getBody());
            if (targets.contains(slug) || targets.contains(title)) {
                out.add(new Usage("ARTICLE_LINK", "Linked from " + other.getTitle(),
                        other.getId(), null, null));
            }
        }

        return new UsageResponse(out);
    }

    /** Article ids referenced by a campaign's play content (beats, sheets, statblocks). */
    public Set<UUID> articleIdsUsedInCampaign(UUID worldId, UUID campaignId) {
        Set<UUID> ids = new HashSet<>();
        List<UUID> arcIds = arcs.findByCampaignIdOrderByPositionAscCreatedAtAsc(campaignId)
                .stream().map(a -> a.getId()).toList();
        if (!arcIds.isEmpty()) {
            ids.addAll(beats.findLinkedArticleIdsByArcIds(arcIds));
        }
        for (CharacterSheet s : sheets.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId)) {
            if (s.getArticleId() != null) {
                ids.add(s.getArticleId());
            }
        }
        for (Statblock s : statblocks.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId)) {
            if (s.getArticleId() != null) {
                ids.add(s.getArticleId());
            }
        }
        return ids;
    }

    private String campaignName(UUID campaignId) {
        return campaignId == null ? null
                : campaigns.findById(campaignId).map(Campaign::getName).orElse(null);
    }
}
