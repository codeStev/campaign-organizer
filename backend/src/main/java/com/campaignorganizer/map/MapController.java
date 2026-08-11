package com.campaignorganizer.map;

import com.campaignorganizer.map.MapDtos.MapRequest;
import com.campaignorganizer.map.MapDtos.MapResponse;
import com.campaignorganizer.media.MediaRepository;
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
@RequestMapping("/api/worlds/{worldId}/maps")
public class MapController {

    private final WorldMapRepository maps;
    private final MediaRepository media;
    private final WorldRepository worlds;

    public MapController(WorldMapRepository maps, MediaRepository media, WorldRepository worlds) {
        this.maps = maps;
        this.media = media;
        this.worlds = worlds;
    }

    @GetMapping
    public List<MapResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return maps.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(MapResponse::from)
                .toList();
    }

    @GetMapping("/{mapId}")
    public MapResponse get(@PathVariable UUID worldId, @PathVariable UUID mapId) {
        return MapResponse.from(findOrThrow(worldId, mapId));
    }

    @PostMapping
    public ResponseEntity<MapResponse> create(@PathVariable UUID worldId,
                                              @Valid @RequestBody MapRequest request) {
        requireWorld(worldId);
        requireImage(worldId, request.mediaId());
        WorldMap saved = maps.save(new WorldMap(worldId, request.name(), request.mediaId()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/maps/" + saved.getId()))
                .body(MapResponse.from(saved));
    }

    @PutMapping("/{mapId}")
    public MapResponse update(@PathVariable UUID worldId, @PathVariable UUID mapId,
                              @Valid @RequestBody MapRequest request) {
        WorldMap map = findOrThrow(worldId, mapId);
        requireImage(worldId, request.mediaId());
        map.update(request.name(), request.mediaId());
        return MapResponse.from(maps.save(map));
    }

    @DeleteMapping("/{mapId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID mapId) {
        maps.delete(findOrThrow(worldId, mapId));
    }

    private WorldMap findOrThrow(UUID worldId, UUID mapId) {
        return maps.findByIdAndWorldId(mapId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Map not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private void requireImage(UUID worldId, UUID mediaId) {
        if (media.findByIdAndWorldId(mediaId, worldId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image not found in this world");
        }
    }
}
