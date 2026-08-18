package com.campaignorganizer.worldbuilding.adapter.relationship.in.web;

import com.campaignorganizer.worldbuilding.adapter.relationship.in.web.RelationshipWebDtos.RelationshipResponse;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.ListArticleRelationshipsUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Relationships touching a single article (its neighbourhood / ego graph). */
@RestController
@RequestMapping("/api/worlds/{worldId}/articles/{articleId}/relationships")
public class ArticleRelationshipController {

    private final ListArticleRelationshipsUseCase listForArticleUseCase;
    private final RelationshipWebMapper mapper;

    public ArticleRelationshipController(ListArticleRelationshipsUseCase listForArticleUseCase,
                                         RelationshipWebMapper mapper) {
        this.listForArticleUseCase = listForArticleUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<RelationshipResponse> list(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        return listForArticleUseCase.listForArticle(worldId, articleId).stream()
                .map(mapper::toResponse).toList();
    }
}
