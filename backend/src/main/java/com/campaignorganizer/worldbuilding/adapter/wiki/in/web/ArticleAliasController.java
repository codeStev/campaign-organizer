package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.application.wiki.port.in.GetArticleAliasesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.SetArticleAliasesUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for an article's aliases (ADR-0116) — same replace-all
 * shape as {@code ArticleTagController}. */
@RestController
@RequestMapping("/api/worlds/{worldId}/articles/{articleId}/aliases")
public class ArticleAliasController {

    /** Same list-of-strings shape for both request and response. */
    public record ArticleAliasesDto(List<String> aliases) {
    }

    private final GetArticleAliasesUseCase getUseCase;
    private final SetArticleAliasesUseCase setUseCase;

    public ArticleAliasController(GetArticleAliasesUseCase getUseCase, SetArticleAliasesUseCase setUseCase) {
        this.getUseCase = getUseCase;
        this.setUseCase = setUseCase;
    }

    @GetMapping
    public ArticleAliasesDto get(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return new ArticleAliasesDto(getUseCase.get(worldId, articleId));
    }

    @PutMapping
    public ArticleAliasesDto set(@PathVariable UUID worldId, @PathVariable UUID articleId,
                                 @RequestBody ArticleAliasesDto request) {
        return new ArticleAliasesDto(setUseCase.set(worldId, articleId, request.aliases()));
    }
}
