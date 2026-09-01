package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.CreateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.UpdateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleListQuery;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CreateArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.DeleteArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.GetArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ListArticleRevisionsUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ListArticlesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.RestoreArticleRevisionUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.UpdateArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRevisionRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleTagLookupPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleImportPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRevisionView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryQueryPort;
import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleTemplate;
import com.campaignorganizer.worldbuilding.domain.wiki.Slugs;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Article + revision use cases; also implements the published article query port. */
@Service
public class ArticleService implements CreateArticleUseCase, UpdateArticleUseCase, DeleteArticleUseCase,
        GetArticleUseCase, ListArticlesUseCase, ListArticleRevisionsUseCase, RestoreArticleRevisionUseCase,
        ArticleQueryPort, ArticleImportPort {

    private final ArticleRepositoryPort articles;
    private final ArticleRevisionRepositoryPort revisions;
    private final CategoryQueryPort categories;
    private final WorldExistsPort worlds;
    private final ArticleTagLookupPort tagLookup;
    private final ArticleViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public ArticleService(ArticleRepositoryPort articles, ArticleRevisionRepositoryPort revisions,
                          CategoryQueryPort categories, WorldExistsPort worlds,
                          ArticleTagLookupPort tagLookup, ArticleViewMapper viewMapper,
                          IdGenerator ids, Clock clock) {
        this.articles = articles;
        this.revisions = revisions;
        this.categories = categories;
        this.worlds = worlds;
        this.tagLookup = tagLookup;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ArticleView create(CreateArticleCommand command) {
        requireWorld(command.worldId());
        validateCategory(command.worldId(), command.categoryId());
        validateParent(command.worldId(), command.parentArticleId(), null);
        String slug = resolveSlugForCreate(command.worldId(), command.slug(), command.title());
        Article created = Article.create(ids.newId(), command.worldId(), command.categoryId(),
                command.parentArticleId(), command.title(), slug, command.template(),
                command.body(), clock.instant());
        return viewMapper.toView(articles.save(created));
    }

    @Override
    @Transactional
    public ArticleView update(UpdateArticleCommand command) {
        Article article = require(command.worldId(), command.articleId());
        validateCategory(command.worldId(), command.categoryId());
        validateParent(command.worldId(), command.parentArticleId(), command.articleId());
        // Snapshot the pre-update state so the change can be reviewed/undone (ADR-0026).
        revisions.save(ArticleRevision.snapshot(ids.newId(), article, clock.instant()));
        String slug = resolveSlugForUpdate(command.worldId(), command.slug(), article);
        article.update(command.categoryId(), command.parentArticleId(), command.title(), slug,
                command.template(), command.body(), clock.instant());
        return viewMapper.toView(articles.save(article));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID articleId) {
        articles.delete(require(worldId, articleId));
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleView get(UUID worldId, UUID articleId) {
        return viewMapper.toView(require(worldId, articleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleSummaryView> list(ArticleListQuery query) {
        requireWorld(query.worldId());
        List<Article> result;
        if (query.restrictToIds() != null) {
            result = articles.findByWorld(query.worldId()).stream()
                    .filter(a -> query.restrictToIds().contains(a.getId()))
                    .toList();
        } else if (query.query() != null && !query.query().isBlank()) {
            String q = query.query().trim();
            List<Article> textMatches = articles.search(query.worldId(), q);
            Set<UUID> matchedIds = textMatches.stream().map(Article::getId)
                    .collect(Collectors.toSet());
            // Tag-only matches (ADR-0087) are appended after the ranked text matches,
            // sorted by recency - not interleaved into the trigram/substring ranking.
            Set<UUID> tagMatchedIds = tagLookup.articleIdsTaggedContaining(query.worldId(), q);
            List<Article> tagOnlyMatches = articles.findByWorld(query.worldId()).stream()
                    .filter(a -> tagMatchedIds.contains(a.getId()) && !matchedIds.contains(a.getId()))
                    .sorted(Comparator.comparing(Article::getUpdatedAt).reversed())
                    .toList();
            result = Stream.concat(textMatches.stream(), tagOnlyMatches.stream()).toList();
        } else if (query.categoryId() != null) {
            result = articles.findByWorldAndCategory(query.worldId(), query.categoryId());
        } else {
            result = articles.findByWorld(query.worldId());
        }
        return result.stream().map(viewMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleRevisionView> list(UUID worldId, UUID articleId) {
        require(worldId, articleId);
        return revisions.findByArticleOrderByCreatedAtDesc(articleId).stream()
                .map(viewMapper::toRevisionView).toList();
    }

    @Override
    @Transactional
    public ArticleView restore(UUID worldId, UUID articleId, UUID revisionId) {
        Article article = require(worldId, articleId);
        ArticleRevision revision = revisions.findByIdAndArticle(revisionId, articleId)
                .orElseThrow(() -> new NotFoundException("Revision not found"));
        // Snapshot current state first so the restore is itself undoable.
        revisions.save(ArticleRevision.snapshot(ids.newId(), article, clock.instant()));
        article.update(article.getCategoryId(), article.getParentArticleId(), revision.getTitle(),
                revision.getSlug(), revision.getTemplate(), revision.getBody(), clock.instant());
        return viewMapper.toView(articles.save(article));
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public ArticleView importArticle(ArticleView view) {
        Article article = Article.reconstitute(view.id(), view.worldId(), view.categoryId(),
                view.parentArticleId(), view.title(), view.slug(),
                ArticleTemplate.valueOf(view.template()), view.body(), view.createdAt(),
                view.updatedAt());
        return viewMapper.toView(articles.save(article));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<ArticleView> findByWorld(UUID worldId) {
        return articles.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticleView> findByIdInWorld(UUID articleId, UUID worldId) {
        return articles.findByIdAndWorld(articleId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ArticleView> findById(UUID articleId) {
        return articles.findById(articleId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsInWorld(articleId, worldId);
    }

    /** Same name resolution as body rendering — one shared index builder. */
    @Override
    @Transactional(readOnly = true)
    public Map<String, UUID> resolveRefs(UUID worldId, Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Map.of();
        }
        var index = ArticleRefIndex.build(articles.findRefsByWorld(worldId));
        return names.stream()
                .filter(index::containsKey)
                .collect(Collectors.toMap(Function.identity(), name -> index.get(name).id()));
    }

    private Article require(UUID worldId, UUID articleId) {
        return articles.findByIdAndWorld(articleId, worldId)
                .orElseThrow(() -> new NotFoundException("Article not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }

    private void validateCategory(UUID worldId, UUID categoryId) {
        if (categoryId != null && !categories.existsInWorld(categoryId, worldId)) {
            throw new ValidationException("Category not found in this world");
        }
    }

    /**
     * Rejects a missing/foreign parent, self-parenting, and multi-hop cycles
     * (walks the proposed parent's ancestor chain looking for selfId). On
     * create, selfId is null and only existence is checked - a brand-new id
     * cannot yet be anyone's ancestor.
     */
    private void validateParent(UUID worldId, UUID parentArticleId, UUID selfId) {
        if (parentArticleId == null) {
            return;
        }
        if (parentArticleId.equals(selfId)) {
            throw new ValidationException("An article cannot be its own parent");
        }
        if (!articles.existsInWorld(parentArticleId, worldId)) {
            throw new ValidationException("Parent article not found in this world");
        }
        if (selfId == null) {
            return;
        }
        UUID cursor = parentArticleId;
        Set<UUID> visited = new HashSet<>();
        while (cursor != null && visited.add(cursor)) {
            if (cursor.equals(selfId)) {
                throw new ValidationException(
                        "Setting this parent would create a cycle in the article hierarchy");
            }
            cursor = articles.findByIdAndWorld(cursor, worldId)
                    .map(Article::getParentArticleId).orElse(null);
        }
    }

    private String resolveSlugForCreate(UUID worldId, String requestedSlug, String title) {
        String base = Slugs.slugify(hasText(requestedSlug) ? requestedSlug : title);
        return Slugs.deduplicate(base, s -> articles.existsSlugInWorld(worldId, s));
    }

    /** Slugs are stable on update; only recomputed when an explicit slug is supplied. */
    private String resolveSlugForUpdate(UUID worldId, String requestedSlug, Article article) {
        if (!hasText(requestedSlug)) {
            return article.getSlug();
        }
        String base = Slugs.slugify(requestedSlug);
        if (base.equals(article.getSlug())) {
            return base;
        }
        return Slugs.deduplicate(base, s -> articles.existsSlugInWorld(worldId, s));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
