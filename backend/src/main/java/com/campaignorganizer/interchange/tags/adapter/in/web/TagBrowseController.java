package com.campaignorganizer.interchange.tags.adapter.in.web;

import com.campaignorganizer.interchange.tags.application.port.in.BrowseTagUseCase;
import com.campaignorganizer.interchange.tags.application.port.in.TagBrowseDtos.TagBrowseResult;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADR-0083: cross-entity browse-by-tag. */
@RestController
@RequestMapping("/api/worlds/{worldId}/tags/{tagName}/entities")
public class TagBrowseController {

    private final BrowseTagUseCase browse;

    public TagBrowseController(BrowseTagUseCase browse) {
        this.browse = browse;
    }

    @GetMapping
    public TagBrowseResult get(@PathVariable UUID worldId, @PathVariable String tagName) {
        return browse.browse(worldId, tagName);
    }
}
