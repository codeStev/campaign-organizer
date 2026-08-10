package com.campaignorganizer.world;

import com.campaignorganizer.world.WorldDtos.WorldRequest;
import com.campaignorganizer.world.WorldDtos.WorldResponse;
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
@RequestMapping("/api/worlds")
public class WorldController {

    private final WorldRepository repository;

    public WorldController(WorldRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<WorldResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(WorldResponse::from)
                .toList();
    }

    @GetMapping("/{worldId}")
    public WorldResponse get(@PathVariable UUID worldId) {
        return WorldResponse.from(findOrThrow(worldId));
    }

    @PostMapping
    public ResponseEntity<WorldResponse> create(@Valid @RequestBody WorldRequest request) {
        World saved = repository.save(new World(request.name(), request.description()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + saved.getId()))
                .body(WorldResponse.from(saved));
    }

    @PutMapping("/{worldId}")
    public WorldResponse update(@PathVariable UUID worldId, @Valid @RequestBody WorldRequest request) {
        World world = findOrThrow(worldId);
        world.update(request.name(), request.description());
        return WorldResponse.from(repository.save(world));
    }

    @DeleteMapping("/{worldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId) {
        World world = findOrThrow(worldId);
        repository.delete(world);
    }

    private World findOrThrow(UUID worldId) {
        return repository.findById(worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found"));
    }
}
