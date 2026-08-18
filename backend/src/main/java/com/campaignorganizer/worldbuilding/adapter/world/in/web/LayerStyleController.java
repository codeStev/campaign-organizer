package com.campaignorganizer.worldbuilding.adapter.world.in.web;

import com.campaignorganizer.worldbuilding.application.world.port.in.GetLayerStylesUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.ReplaceLayerStylesUseCase;
import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Persisted per-layer map styling for a world (ADR-0049). */
@RestController
@RequestMapping("/api/worlds/{worldId}/layer-styles")
public class LayerStyleController {

    private final GetLayerStylesUseCase getUseCase;
    private final ReplaceLayerStylesUseCase replaceUseCase;

    public LayerStyleController(GetLayerStylesUseCase getUseCase,
                               ReplaceLayerStylesUseCase replaceUseCase) {
        this.getUseCase = getUseCase;
        this.replaceUseCase = replaceUseCase;
    }

    @GetMapping
    public Map<String, LayerStyle> get(@PathVariable UUID worldId) {
        return getUseCase.getLayerStyles(worldId);
    }

    @PutMapping
    public Map<String, LayerStyle> put(@PathVariable UUID worldId,
                                       @RequestBody Map<String, LayerStyle> styles) {
        return replaceUseCase.replace(worldId, styles);
    }
}
