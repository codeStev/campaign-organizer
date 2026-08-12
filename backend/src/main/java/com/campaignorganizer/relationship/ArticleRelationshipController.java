package com.campaignorganizer.relationship;

import com.campaignorganizer.relationship.RelationshipDtos.RelationshipResponse;
import com.campaignorganizer.wiki.ArticleRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Relationships touching a single article (its neighbourhood / ego graph). */
@RestController
@RequestMapping("/api/worlds/{worldId}/articles/{articleId}/relationships")
public class ArticleRelationshipController {

    private final RelationshipRepository relationships;
    private final ArticleRepository articles;

    public ArticleRelationshipController(RelationshipRepository relationships, ArticleRepository articles) {
        this.relationships = relationships;
        this.articles = articles;
    }

    @GetMapping
    public List<RelationshipResponse> list(@PathVariable UUID worldId, @PathVariable UUID articleId) {
        if (!articles.existsByIdAndWorldId(articleId, worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        return relationships.findTouchingArticle(worldId, articleId).stream()
                .map(RelationshipResponse::from)
                .toList();
    }
}
