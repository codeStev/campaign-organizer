package com.campaignorganizer.interchange.tags.application.service;

import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.interchange.tags.application.port.in.BrowseTagUseCase;
import com.campaignorganizer.interchange.tags.application.port.in.TagBrowseDtos.ArticleSummary;
import com.campaignorganizer.interchange.tags.application.port.in.TagBrowseDtos.StatblockSummary;
import com.campaignorganizer.interchange.tags.application.port.in.TagBrowseDtos.TagBrowseResult;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tagging.domain.EntityType;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cross-entity browse-by-tag (ADR-0083): composes tagging's published query
 * port with worldbuilding's and characters' published query ports, the same
 * orchestration role {@code ConsistencyReportService} plays for FR-43. Holds
 * no core domain rules of its own.
 */
@Service
public class TagBrowseService implements BrowseTagUseCase {

    private final WorldQueryPort worlds;
    private final TagQueryPort tags;
    private final ArticleQueryPort articles;
    private final StatblockQueryPort statblocks;

    public TagBrowseService(WorldQueryPort worlds, TagQueryPort tags, ArticleQueryPort articles,
                            StatblockQueryPort statblocks) {
        this.worlds = worlds;
        this.tags = tags;
        this.articles = articles;
        this.statblocks = statblocks;
    }

    @Override
    @Transactional(readOnly = true)
    public TagBrowseResult browse(UUID worldId, String tag) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
        Set<UUID> articleIds = Set.copyOf(
                tags.entityIdsTaggedWith(worldId, EntityType.ARTICLE, tag));
        Set<UUID> statblockIds = Set.copyOf(
                tags.entityIdsTaggedWith(worldId, EntityType.STATBLOCK, tag));

        List<ArticleSummary> taggedArticles = articles.findByWorld(worldId).stream()
                .filter(a -> articleIds.contains(a.id()))
                .map(TagBrowseService::toSummary)
                .toList();
        List<StatblockSummary> taggedStatblocks = statblocks.findByWorld(worldId).stream()
                .filter(s -> statblockIds.contains(s.id()))
                .map(TagBrowseService::toSummary)
                .toList();

        return new TagBrowseResult(tag, taggedArticles, taggedStatblocks);
    }

    private static ArticleSummary toSummary(ArticleView a) {
        return new ArticleSummary(a.id(), a.worldId(), a.categoryId(), a.parentArticleId(),
                a.title(), a.slug(), a.template(), a.createdAt(), a.updatedAt());
    }

    private static StatblockSummary toSummary(StatblockView s) {
        return new StatblockSummary(s.id(), s.worldId(), s.articleId(), s.campaignId(),
                s.worldTemplateId(), s.globalTemplateId(), s.name(), s.stats(), s.notes(), s.createdAt(),
                s.updatedAt());
    }
}
