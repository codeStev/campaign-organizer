package com.campaignorganizer.statblock;

import com.campaignorganizer.statblock.StatblockDtos.StatblockRequest;
import com.campaignorganizer.statblock.StatblockDtos.StatblockResponse;
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
@RequestMapping("/api/worlds/{worldId}/statblocks")
public class StatblockController {

    private final StatblockRepository statblocks;
    private final ArticleRepository articles;
    private final WorldRepository worlds;

    public StatblockController(StatblockRepository statblocks, ArticleRepository articles,
                              WorldRepository worlds) {
        this.statblocks = statblocks;
        this.articles = articles;
        this.worlds = worlds;
    }

    @GetMapping
    public List<StatblockResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return statblocks.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(StatblockResponse::from)
                .toList();
    }

    @GetMapping("/{statblockId}")
    public StatblockResponse get(@PathVariable UUID worldId, @PathVariable UUID statblockId) {
        return StatblockResponse.from(findOrThrow(worldId, statblockId));
    }

    @PostMapping
    public ResponseEntity<StatblockResponse> create(@PathVariable UUID worldId,
                                                    @Valid @RequestBody StatblockRequest request) {
        requireWorld(worldId);
        validateArticle(worldId, request.articleId());
        Statblock saved = statblocks.save(new Statblock(worldId, request.articleId(),
                request.name(), request.stats(), request.notes()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/statblocks/" + saved.getId()))
                .body(StatblockResponse.from(saved));
    }

    @PutMapping("/{statblockId}")
    public StatblockResponse update(@PathVariable UUID worldId, @PathVariable UUID statblockId,
                                    @Valid @RequestBody StatblockRequest request) {
        Statblock statblock = findOrThrow(worldId, statblockId);
        validateArticle(worldId, request.articleId());
        statblock.update(request.articleId(), request.name(), request.stats(), request.notes());
        return StatblockResponse.from(statblocks.save(statblock));
    }

    @DeleteMapping("/{statblockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID statblockId) {
        statblocks.delete(findOrThrow(worldId, statblockId));
    }

    private Statblock findOrThrow(UUID worldId, UUID statblockId) {
        return statblocks.findByIdAndWorldId(statblockId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statblock not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private void validateArticle(UUID worldId, UUID articleId) {
        if (articleId != null && !articles.existsByIdAndWorldId(articleId, worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article not found in this world");
        }
    }
}
