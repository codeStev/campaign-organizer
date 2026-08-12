package com.campaignorganizer.wiki;

import com.campaignorganizer.wiki.ArticleDtos.ArticleResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/articles/{articleId}/revisions")
public class ArticleRevisionController {

    public record RevisionResponse(UUID id, UUID articleId, String title, String slug,
                                   ArticleTemplate template, String body, Instant createdAt) {
        static RevisionResponse from(ArticleRevision r) {
            return new RevisionResponse(r.getId(), r.getArticleId(), r.getTitle(), r.getSlug(),
                    r.getTemplate(), r.getBody(), r.getCreatedAt());
        }
    }

    private final ArticleRevisionRepository revisions;
    private final ArticleRepository articles;
    private final AutoLinker autoLinker;

    public ArticleRevisionController(ArticleRevisionRepository revisions, ArticleRepository articles,
                                     AutoLinker autoLinker) {
        this.revisions = revisions;
        this.articles = articles;
        this.autoLinker = autoLinker;
    }

    @GetMapping
    public List<RevisionResponse> list(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        requireArticle(worldId, articleId);
        return revisions.findByArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(RevisionResponse::from)
                .toList();
    }

    @PostMapping("/{revisionId}/restore")
    public ArticleResponse restore(@PathVariable UUID worldId, @PathVariable UUID articleId,
                                   @PathVariable UUID revisionId) {
        Article article = requireArticle(worldId, articleId);
        ArticleRevision revision = revisions.findByIdAndArticleId(revisionId, articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revision not found"));
        // Snapshot current state first so the restore is itself undoable.
        revisions.save(ArticleRevision.of(article));
        article.update(article.getCategoryId(), revision.getTitle(), revision.getSlug(),
                revision.getTemplate(), revision.getBody());
        Article saved = articles.save(article);
        return ArticleResponse.from(saved, autoLinker.render(saved.getWorldId(), saved.getBody()));
    }

    private Article requireArticle(UUID worldId, UUID articleId) {
        return articles.findByIdAndWorldId(articleId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
    }
}
