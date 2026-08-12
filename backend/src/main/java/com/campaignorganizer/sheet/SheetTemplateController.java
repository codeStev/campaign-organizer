package com.campaignorganizer.sheet;

import com.campaignorganizer.sheet.SheetTemplateDtos.SheetTemplateRequest;
import com.campaignorganizer.sheet.SheetTemplateDtos.SheetTemplateResponse;
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
@RequestMapping("/api/worlds/{worldId}/sheet-templates")
public class SheetTemplateController {

    private final SheetTemplateRepository templates;
    private final WorldRepository worlds;

    public SheetTemplateController(SheetTemplateRepository templates, WorldRepository worlds) {
        this.templates = templates;
        this.worlds = worlds;
    }

    @GetMapping
    public List<SheetTemplateResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return templates.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(SheetTemplateResponse::from)
                .toList();
    }

    @GetMapping("/{templateId}")
    public SheetTemplateResponse get(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        return SheetTemplateResponse.from(findOrThrow(worldId, templateId));
    }

    @PostMapping
    public ResponseEntity<SheetTemplateResponse> create(@PathVariable UUID worldId,
                                                        @Valid @RequestBody SheetTemplateRequest request) {
        requireWorld(worldId);
        SheetTemplate saved = templates.save(
                new SheetTemplate(worldId, request.name(), request.system(), request.sections()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/sheet-templates/" + saved.getId()))
                .body(SheetTemplateResponse.from(saved));
    }

    @PutMapping("/{templateId}")
    public SheetTemplateResponse update(@PathVariable UUID worldId, @PathVariable UUID templateId,
                                        @Valid @RequestBody SheetTemplateRequest request) {
        SheetTemplate template = findOrThrow(worldId, templateId);
        template.update(request.name(), request.system(), request.sections());
        return SheetTemplateResponse.from(templates.save(template));
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        templates.delete(findOrThrow(worldId, templateId));
    }

    private SheetTemplate findOrThrow(UUID worldId, UUID templateId) {
        return templates.findByIdAndWorldId(templateId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sheet template not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }
}
