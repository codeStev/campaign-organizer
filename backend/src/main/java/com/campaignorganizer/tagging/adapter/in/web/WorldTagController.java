package com.campaignorganizer.tagging.adapter.in.web;

import com.campaignorganizer.tagging.application.port.in.ListWorldTagsUseCase;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for a world's distinct tag names (ADR-0083, autocomplete source). */
@RestController
@RequestMapping("/api/worlds/{worldId}/tags")
public class WorldTagController {

    private final ListWorldTagsUseCase listUseCase;

    public WorldTagController(ListWorldTagsUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    @GetMapping
    public List<String> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId);
    }
}
