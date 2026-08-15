package com.campaignorganizer.world;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Persisted per-layer map styling for a world (ADR-0049). */
@RestController
@RequestMapping("/api/worlds/{worldId}/layer-styles")
public class LayerStyleController {

    private final WorldRepository worlds;

    public LayerStyleController(WorldRepository worlds) {
        this.worlds = worlds;
    }

    @GetMapping
    public Map<String, LayerStyle> get(@PathVariable UUID worldId) {
        return requireWorld(worldId).getLayerStyles();
    }

    @PutMapping
    public Map<String, LayerStyle> put(@PathVariable UUID worldId,
                                       @RequestBody Map<String, LayerStyle> styles) {
        World world = requireWorld(worldId);
        world.setLayerStyles(styles);
        return worlds.save(world).getLayerStyles();
    }

    private World requireWorld(UUID worldId) {
        return worlds.findById(worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found"));
    }
}
