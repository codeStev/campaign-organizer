package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleResponse;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ApplyAutolinksUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.AutolinkDtos.AutolinkCandidateGroup;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.AutolinkDtos.AutolinkSelection;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ScanAutolinkCandidatesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Web adapter for the auto-link scan/apply (ADR-0116). */
@RestController
@RequestMapping("/api/worlds/{worldId}/articles")
public class AutolinkController {

    /** Request body shape for the apply endpoint. */
    public record ApplyAutolinksRequest(List<AutolinkSelection> selections) {
    }

    private final ScanAutolinkCandidatesUseCase scanUseCase;
    private final ApplyAutolinksUseCase applyUseCase;
    private final ArticleRenderPort renderPort;
    private final ArticleWebMapper mapper;

    public AutolinkController(ScanAutolinkCandidatesUseCase scanUseCase, ApplyAutolinksUseCase applyUseCase,
                              ArticleRenderPort renderPort, ArticleWebMapper mapper) {
        this.scanUseCase = scanUseCase;
        this.applyUseCase = applyUseCase;
        this.renderPort = renderPort;
        this.mapper = mapper;
    }

    @GetMapping("/autolink-candidates")
    public List<AutolinkCandidateGroup> scan(@PathVariable UUID worldId) {
        return scanUseCase.scan(worldId);
    }

    @PostMapping("/{articleId}/autolink")
    public ArticleResponse apply(@PathVariable UUID worldId, @PathVariable UUID articleId,
                                 @RequestBody ApplyAutolinksRequest request) {
        ArticleView updated = applyUseCase.apply(worldId, articleId, request.selections());
        return mapper.toResponse(updated, renderPort.renderBody(updated.worldId(), updated.body()));
    }
}
