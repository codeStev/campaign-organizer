package com.campaignorganizer.worldbuilding.adapter.world.in.web;

import com.campaignorganizer.worldbuilding.adapter.world.in.web.WorldWebDtos.WorldRequest;
import com.campaignorganizer.worldbuilding.adapter.world.in.web.WorldWebDtos.WorldResponse;
import com.campaignorganizer.worldbuilding.application.world.port.in.CreateWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.DeleteWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.GetWorldUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.ListWorldsUseCase;
import com.campaignorganizer.worldbuilding.application.world.port.in.UpdateWorldUseCase;
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

@RestController
@RequestMapping("/api/worlds")
public class WorldController {

    private final CreateWorldUseCase createUseCase;
    private final UpdateWorldUseCase updateUseCase;
    private final DeleteWorldUseCase deleteUseCase;
    private final GetWorldUseCase getUseCase;
    private final ListWorldsUseCase listUseCase;
    private final WorldWebMapper mapper;

    public WorldController(CreateWorldUseCase createUseCase, UpdateWorldUseCase updateUseCase,
                          DeleteWorldUseCase deleteUseCase, GetWorldUseCase getUseCase,
                          ListWorldsUseCase listUseCase, WorldWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<WorldResponse> list() {
        return listUseCase.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{worldId}")
    public WorldResponse get(@PathVariable UUID worldId) {
        return mapper.toResponse(getUseCase.get(worldId));
    }

    @PostMapping
    public ResponseEntity<WorldResponse> create(@Valid @RequestBody WorldRequest request) {
        WorldResponse response = mapper.toResponse(createUseCase.create(mapper.toCreateCommand(request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + response.id()))
                .body(response);
    }

    @PutMapping("/{worldId}")
    public WorldResponse update(@PathVariable UUID worldId, @Valid @RequestBody WorldRequest request) {
        return mapper.toResponse(updateUseCase.update(mapper.toUpdateCommand(worldId, request)));
    }

    @DeleteMapping("/{worldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId) {
        deleteUseCase.delete(worldId);
    }
}
