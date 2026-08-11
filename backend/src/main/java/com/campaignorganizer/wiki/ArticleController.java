package com.campaignorganizer.wiki;

import com.campaignorganizer.wiki.ArticleDtos.ArticleRequest;
import com.campaignorganizer.wiki.ArticleDtos.ArticleResponse;
import com.campaignorganizer.wiki.ArticleDtos.ArticleSummaryResponse;
import com.campaignorganizer.world.WorldRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/articles")
public class ArticleController {

    private final ArticleRepository articles;
    private final CategoryRepository categories;
    private final WorldRepository worlds;
    private final AutoLinker autoLinker;

    public ArticleController(ArticleRepository articles, CategoryRepository categories,
                             WorldRepository worlds, AutoLinker autoLinker) {
        this.articles = articles;
        this.categories = categories;
        this.worlds = worlds;
        this.autoLinker = autoLinker;
    }

    @GetMapping
    public List<ArticleSummaryResponse> list(@PathVariable UUID worldId,
                                             @RequestParam(required = false) UUID categoryId,
                                             @RequestParam(required = false) String q) {
        requireWorld(worldId);
        List<Article> result;
        if (q != null && !q.isBlank()) {
            result = articles.search(worldId, q.trim());
        } else if (categoryId != null) {
            result = articles.findByWorldIdAndCategoryIdOrderByCreatedAtDesc(worldId, categoryId);
        } else {
            result = articles.findByWorldIdOrderByCreatedAtDesc(worldId);
        }
        return result.stream().map(ArticleSummaryResponse::from).toList();
    }

    @GetMapping("/{articleId}")
    public ArticleResponse get(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return toResponse(findOrThrow(worldId, articleId));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> create(@PathVariable UUID worldId,
                                                  @Valid @RequestBody ArticleRequest request) {
        requireWorld(worldId);
        validateCategory(worldId, request.categoryId());
        ArticleTemplate template = request.template() == null ? ArticleTemplate.GENERIC : request.template();
        String slug = resolveSlugForCreate(worldId, request);
        Article saved = articles.save(new Article(
                worldId, request.categoryId(), request.title(), slug, template, request.body()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/articles/" + saved.getId()))
                .body(toResponse(saved));
    }

    @PutMapping("/{articleId}")
    public ArticleResponse update(@PathVariable UUID worldId, @PathVariable UUID articleId,
                                  @Valid @RequestBody ArticleRequest request) {
        Article article = findOrThrow(worldId, articleId);
        validateCategory(worldId, request.categoryId());
        ArticleTemplate template = request.template() == null ? ArticleTemplate.GENERIC : request.template();
        String slug = resolveSlugForUpdate(worldId, request, article);
        article.update(request.categoryId(), request.title(), slug, template, request.body());
        return toResponse(articles.save(article));
    }

    @DeleteMapping("/{articleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        articles.delete(findOrThrow(worldId, articleId));
    }

    private ArticleResponse toResponse(Article article) {
        return ArticleResponse.from(article, autoLinker.render(article.getWorldId(), article.getBody()));
    }

    private Article findOrThrow(UUID worldId, UUID articleId) {
        return articles.findByIdAndWorldId(articleId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private void validateCategory(UUID worldId, UUID categoryId) {
        if (categoryId != null && !categories.existsByIdAndWorldId(categoryId, worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found in this world");
        }
    }

    private String resolveSlugForCreate(UUID worldId, ArticleRequest request) {
        String base = Slugs.slugify(hasText(request.slug()) ? request.slug() : request.title());
        return Slugs.deduplicate(base, s -> articles.existsByWorldIdAndSlug(worldId, s));
    }

    /** Slugs are stable on update; only recomputed when an explicit slug is supplied. */
    private String resolveSlugForUpdate(UUID worldId, ArticleRequest request, Article article) {
        if (!hasText(request.slug())) {
            return article.getSlug();
        }
        String base = Slugs.slugify(request.slug());
        if (base.equals(article.getSlug())) {
            return base;
        }
        return Slugs.deduplicate(base, s -> articles.existsByWorldIdAndSlug(worldId, s));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
