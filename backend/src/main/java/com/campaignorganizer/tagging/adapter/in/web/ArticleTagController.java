package com.campaignorganizer.tagging.adapter.in.web;

import com.campaignorganizer.tagging.adapter.in.web.TagWebDtos.EntityTagsRequest;
import com.campaignorganizer.tagging.adapter.in.web.TagWebDtos.EntityTagsResponse;
import com.campaignorganizer.tagging.application.port.in.ListEntityTagsUseCase;
import com.campaignorganizer.tagging.application.port.in.SetEntityTagsUseCase;
import com.campaignorganizer.tagging.domain.EntityType;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for an article's tags (ADR-0083). */
@RestController
@RequestMapping("/api/worlds/{worldId}/articles/{articleId}/tags")
public class ArticleTagController {

    private final ListEntityTagsUseCase listUseCase;
    private final SetEntityTagsUseCase setUseCase;
    private final TagWebMapper mapper;

    public ArticleTagController(ListEntityTagsUseCase listUseCase, SetEntityTagsUseCase setUseCase,
                                TagWebMapper mapper) {
        this.listUseCase = listUseCase;
        this.setUseCase = setUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public EntityTagsResponse get(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return mapper.toResponse(listUseCase.list(worldId, EntityType.ARTICLE, articleId));
    }

    @PutMapping
    public EntityTagsResponse set(@PathVariable UUID worldId, @PathVariable UUID articleId,
                                  @Valid @RequestBody EntityTagsRequest request) {
        return mapper.toResponse(setUseCase.set(
                mapper.toSetCommand(worldId, EntityType.ARTICLE, articleId, request)));
    }
}
