package com.campaignorganizer.whiteboard;

import com.campaignorganizer.whiteboard.WhiteboardDtos.WhiteboardRequest;
import com.campaignorganizer.whiteboard.WhiteboardDtos.WhiteboardResponse;
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
@RequestMapping("/api/worlds/{worldId}/whiteboards")
public class WhiteboardController {

    private final WhiteboardRepository whiteboards;
    private final WorldRepository worlds;

    public WhiteboardController(WhiteboardRepository whiteboards, WorldRepository worlds) {
        this.whiteboards = whiteboards;
        this.worlds = worlds;
    }

    @GetMapping
    public List<WhiteboardResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return whiteboards.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(WhiteboardResponse::from)
                .toList();
    }

    @GetMapping("/{whiteboardId}")
    public WhiteboardResponse get(@PathVariable UUID worldId, @PathVariable UUID whiteboardId) {
        return WhiteboardResponse.from(findOrThrow(worldId, whiteboardId));
    }

    @PostMapping
    public ResponseEntity<WhiteboardResponse> create(@PathVariable UUID worldId,
                                                     @Valid @RequestBody WhiteboardRequest request) {
        requireWorld(worldId);
        Whiteboard saved = whiteboards.save(
                new Whiteboard(worldId, request.name(), request.nodes(), request.edges()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/whiteboards/" + saved.getId()))
                .body(WhiteboardResponse.from(saved));
    }

    @PutMapping("/{whiteboardId}")
    public WhiteboardResponse update(@PathVariable UUID worldId, @PathVariable UUID whiteboardId,
                                     @Valid @RequestBody WhiteboardRequest request) {
        Whiteboard board = findOrThrow(worldId, whiteboardId);
        board.update(request.name(), request.nodes(), request.edges());
        return WhiteboardResponse.from(whiteboards.save(board));
    }

    @DeleteMapping("/{whiteboardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID whiteboardId) {
        whiteboards.delete(findOrThrow(worldId, whiteboardId));
    }

    private Whiteboard findOrThrow(UUID worldId, UUID whiteboardId) {
        return whiteboards.findByIdAndWorldId(whiteboardId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Whiteboard not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }
}
