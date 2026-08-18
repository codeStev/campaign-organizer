package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.domain.wiki.ArticleTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ArticleWebDtos {

    private ArticleWebDtos() {
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
    }

    public record RevisionResponse(
            UUID id,
            UUID articleId,
            String title,
            String slug,
            ArticleTemplate template,
            String body,
            Instant createdAt) {
    }
}
