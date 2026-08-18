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
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRevisionView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryQueryPort;
import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import com.campaignorganizer.worldbuilding.domain.wiki.HtmlSanitizer;
import com.campaignorganizer.worldbuilding.domain.wiki.Slugs;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Article + revision use cases; also implements the published article query port. */
@Service
public class ArticleService implements CreateArticleUseCase, UpdateArticleUseCase, DeleteArticleUseCase,
        GetArticleUseCase, ListArticlesUseCase, ListArticleRevisionsUseCase, RestoreArticleRevisionUseCase,
        ArticleQueryPort {

    private final ArticleRepositoryPort articles;
    private final ArticleRevisionRepositoryPort revisions;
    private final CategoryQueryPort categories;
    private final WorldExistsPort worlds;
    private final ArticleViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;
    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    public ArticleService(ArticleRepositoryPort articles, ArticleRevisionRepositoryPort revisions,
                          CategoryQueryPort categories, WorldExistsPort worlds,
                          ArticleViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.articles = articles;
        this.revisions = revisions;
        this.categories = categories;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ArticleView create(CreateArticleCommand command) {
        requireWorld(command.worldId());
        validateCategory(command.worldId(), command.categoryId());
        String slug = resolveSlugForCreate(command.worldId(), command.slug(), command.title());
        Article created = Article.create(ids.newId(), command.worldId(), command.categoryId(),
                command.title(), slug, command.template(), sanitizer.sanitize(command.body()),
                clock.instant());
        return viewMapper.toView(articles.save(created));
    }

    @Override
    @Transactional
    public ArticleView update(UpdateArticleCommand command) {
        Article article = require(command.worldId(), command.articleId());
        validateCategory(command.worldId(), command.categoryId());
        // Snapshot the pre-update state so the change can be reviewed/undone (ADR-0026).
        revisions.save(ArticleRevision.snapshot(ids.newId(), article, clock.instant()));
        String slug = resolveSlugForUpdate(command.worldId(), command.slug(), article);
        article.update(command.categoryId(), command.title(), slug, command.template(),
                sanitizer.sanitize(command.body()), clock.instant());
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
            result = articles.search(query.worldId(), query.query().trim());
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
        article.update(article.getCategoryId(), revision.getTitle(), revision.getSlug(),
                revision.getTemplate(), revision.getBody(), clock.instant());
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
