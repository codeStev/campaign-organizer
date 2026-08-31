package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleRequest;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleResponse;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleSummaryResponse;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleListQuery;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CreateArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.DeleteArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.GetArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ListArticlesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.UpdateArticleUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleTagLookupPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.CampaignArticleUsagePort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

@RestController
@RequestMapping("/api/worlds/{worldId}/articles")
public class ArticleController {

    private final CreateArticleUseCase createUseCase;
    private final UpdateArticleUseCase updateUseCase;
    private final DeleteArticleUseCase deleteUseCase;
    private final GetArticleUseCase getUseCase;
    private final ListArticlesUseCase listUseCase;
    private final ArticleRenderPort renderPort;
    private final CampaignArticleUsagePort campaignUsage;
    private final ArticleTagLookupPort tagLookup;
    private final ArticleWebMapper mapper;

    public ArticleController(CreateArticleUseCase createUseCase, UpdateArticleUseCase updateUseCase,
                             DeleteArticleUseCase deleteUseCase, GetArticleUseCase getUseCase,
                             ListArticlesUseCase listUseCase, ArticleRenderPort renderPort,
                             CampaignArticleUsagePort campaignUsage, ArticleTagLookupPort tagLookup,
                             ArticleWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.renderPort = renderPort;
        this.campaignUsage = campaignUsage;
        this.tagLookup = tagLookup;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ArticleSummaryResponse> list(@PathVariable UUID worldId,
                                             @RequestParam(required = false) UUID categoryId,
                                             @RequestParam(required = false) UUID campaignId,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) String tag) {
        Set<UUID> restrictToIds = campaignId == null
                ? null
                : campaignUsage.articleIdsUsedInCampaign(worldId, campaignId);
        if (tag != null && !tag.isBlank()) {
            Set<UUID> tagged = tagLookup.articleIdsTaggedWith(worldId, tag);
            restrictToIds = restrictToIds == null
                    ? tagged
                    : restrictToIds.stream().filter(tagged::contains).collect(Collectors.toSet());
        }
        return listUseCase.list(new ArticleListQuery(worldId, categoryId, q, restrictToIds)).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @GetMapping("/{articleId}")
    public ArticleResponse get(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return toResponse(getUseCase.get(worldId, articleId));
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> create(@PathVariable UUID worldId,
                                                  @Valid @RequestBody ArticleRequest request) {
        ArticleResponse response = toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/articles/" + response.id()))
                .body(response);
    }

    @PutMapping("/{articleId}")
    public ArticleResponse update(@PathVariable UUID worldId, @PathVariable UUID articleId,
                                  @Valid @RequestBody ArticleRequest request) {
        return toResponse(updateUseCase.update(mapper.toUpdateCommand(worldId, articleId, request)));
    }

    @DeleteMapping("/{articleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        deleteUseCase.delete(worldId, articleId);
    }

    private ArticleResponse toResponse(ArticleView view) {
        return mapper.toResponse(view, renderPort.renderBody(view.worldId(), view.body()));
    }
}
