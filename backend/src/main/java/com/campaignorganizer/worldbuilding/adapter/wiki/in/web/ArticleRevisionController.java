package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleResponse;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.RevisionResponse;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ListArticleRevisionsUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.RestoreArticleRevisionUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worlds/{worldId}/articles/{articleId}/revisions")
public class ArticleRevisionController {

    private final ListArticleRevisionsUseCase listUseCase;
    private final RestoreArticleRevisionUseCase restoreUseCase;
    private final ArticleRenderPort renderPort;
    private final ArticleWebMapper mapper;

    public ArticleRevisionController(ListArticleRevisionsUseCase listUseCase,
                                     RestoreArticleRevisionUseCase restoreUseCase,
                                     ArticleRenderPort renderPort, ArticleWebMapper mapper) {
        this.listUseCase = listUseCase;
        this.restoreUseCase = restoreUseCase;
        this.renderPort = renderPort;
        this.mapper = mapper;
    }

    @GetMapping
    public List<RevisionResponse> list(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return listUseCase.list(worldId, articleId).stream().map(mapper::toRevisionResponse).toList();
    }

    @PostMapping("/{revisionId}/restore")
    public ArticleResponse restore(@PathVariable UUID worldId, @PathVariable UUID articleId,
                                   @PathVariable UUID revisionId) {
        ArticleView view = restoreUseCase.restore(worldId, articleId, revisionId);
        return mapper.toResponse(view, renderPort.renderBody(view.worldId(), view.body()));
    }
}
