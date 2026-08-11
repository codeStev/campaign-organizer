package com.campaignorganizer.map;

import com.campaignorganizer.map.MapPinDtos.MapPinRequest;
import com.campaignorganizer.map.MapPinDtos.MapPinResponse;
import com.campaignorganizer.wiki.ArticleRepository;
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
@RequestMapping("/api/worlds/{worldId}/maps/{mapId}/pins")
public class MapPinController {

    private final MapPinRepository pins;
    private final WorldMapRepository maps;
    private final ArticleRepository articles;

    public MapPinController(MapPinRepository pins, WorldMapRepository maps, ArticleRepository articles) {
        this.pins = pins;
        this.maps = maps;
        this.articles = articles;
    }

    @GetMapping
    public List<MapPinResponse> list(@PathVariable UUID worldId, @PathVariable UUID mapId) {
        requireMap(worldId, mapId);
        return pins.findByMapIdOrderByCreatedAtAsc(mapId).stream()
                .map(MapPinResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<MapPinResponse> create(@PathVariable UUID worldId, @PathVariable UUID mapId,
                                                 @Valid @RequestBody MapPinRequest request) {
        requireMap(worldId, mapId);
        validateArticle(worldId, request.articleId());
        MapPin saved = pins.save(new MapPin(mapId, request.articleId(), request.label(),
                request.layer(), request.x(), request.y()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/maps/" + mapId + "/pins/" + saved.getId()))
                .body(MapPinResponse.from(saved));
    }

    @PutMapping("/{pinId}")
    public MapPinResponse update(@PathVariable UUID worldId, @PathVariable UUID mapId,
                                 @PathVariable UUID pinId, @Valid @RequestBody MapPinRequest request) {
        requireMap(worldId, mapId);
        validateArticle(worldId, request.articleId());
        MapPin pin = pins.findByIdAndMapId(pinId, mapId)
                .orElseThrow(this::pinNotFound);
        pin.update(request.articleId(), request.label(), request.layer(), request.x(), request.y());
        return MapPinResponse.from(pins.save(pin));
    }

    @DeleteMapping("/{pinId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID mapId, @PathVariable UUID pinId) {
        requireMap(worldId, mapId);
        MapPin pin = pins.findByIdAndMapId(pinId, mapId)
                .orElseThrow(this::pinNotFound);
        pins.delete(pin);
    }

    private void requireMap(UUID worldId, UUID mapId) {
        if (!maps.existsByIdAndWorldId(mapId, worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Map not found");
        }
    }

    private void validateArticle(UUID worldId, UUID articleId) {
        if (articleId != null && !articles.existsByIdAndWorldId(articleId, worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article not found in this world");
        }
    }

    private ResponseStatusException pinNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin not found");
    }
}
