package com.campaignorganizer.relationship;

import com.campaignorganizer.relationship.RelationshipDtos.RelationshipRequest;
import com.campaignorganizer.relationship.RelationshipDtos.RelationshipResponse;
import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.world.WorldRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/relationships")
public class RelationshipController {

    private final RelationshipRepository relationships;
    private final ArticleRepository articles;
    private final WorldRepository worlds;

    public RelationshipController(RelationshipRepository relationships, ArticleRepository articles,
                                  WorldRepository worlds) {
        this.relationships = relationships;
        this.articles = articles;
        this.worlds = worlds;
    }

    @GetMapping
    public List<RelationshipResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return relationships.findByWorldIdOrderByCreatedAtAsc(worldId).stream()
                .map(RelationshipResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<RelationshipResponse> create(@PathVariable UUID worldId,
                                                       @Valid @RequestBody RelationshipRequest request) {
        requireWorld(worldId);
        validateEndpoints(worldId, request);
        Relationship saved = relationships.save(new Relationship(worldId, request.fromArticleId(),
                request.toArticleId(), request.label(), request.directedOrDefault()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/relationships/" + saved.getId()))
                .body(RelationshipResponse.from(saved));
    }

    @PutMapping("/{relationshipId}")
    public RelationshipResponse update(@PathVariable UUID worldId, @PathVariable UUID relationshipId,
                                       @Valid @RequestBody RelationshipRequest request) {
        Relationship relationship = findOrThrow(worldId, relationshipId);
        validateEndpoints(worldId, request);
        relationship.update(request.fromArticleId(), request.toArticleId(),
                request.label(), request.directedOrDefault());
        return RelationshipResponse.from(relationships.save(relationship));
    }

    @DeleteMapping("/{relationshipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID relationshipId) {
        relationships.delete(findOrThrow(worldId, relationshipId));
    }

    private Relationship findOrThrow(UUID worldId, UUID relationshipId) {
        return relationships.findByIdAndWorldId(relationshipId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Relationship not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private void validateEndpoints(UUID worldId, RelationshipRequest request) {
        if (request.fromArticleId().equals(request.toArticleId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An article cannot relate to itself");
        }
        if (!articles.existsByIdAndWorldId(request.fromArticleId(), worldId)
                || !articles.existsByIdAndWorldId(request.toArticleId(), worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both articles must exist in this world");
        }
    }
}
