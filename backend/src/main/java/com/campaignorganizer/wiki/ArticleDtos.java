package com.campaignorganizer.wiki;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ArticleDtos {

    private ArticleDtos() {
    }

    public record ArticleRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 220) String slug,
            ArticleTemplate template,
            UUID categoryId,
            String body) {
    }

    /** List view: no body, to keep listings light. */
    public record ArticleSummaryResponse(
            UUID id,
            UUID worldId,
            UUID categoryId,
            String title,
            String slug,
            ArticleTemplate template,
            Instant createdAt,
            Instant updatedAt) {

        public static ArticleSummaryResponse from(Article a) {
            return new ArticleSummaryResponse(a.getId(), a.getWorldId(), a.getCategoryId(),
                    a.getTitle(), a.getSlug(), a.getTemplate(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    /** Detail view: raw body plus the auto-linked rendering (ADR-0014). */
    public record ArticleResponse(
            UUID id,
            UUID worldId,
            UUID categoryId,
            String title,
            String slug,
            ArticleTemplate template,
            String body,
            String bodyHtml,
            Instant createdAt,
            Instant updatedAt) {

        public static ArticleResponse from(Article a, String bodyHtml) {
            return new ArticleResponse(a.getId(), a.getWorldId(), a.getCategoryId(),
                    a.getTitle(), a.getSlug(), a.getTemplate(), a.getBody(), bodyHtml,
                    a.getCreatedAt(), a.getUpdatedAt());
        }
    }
}
