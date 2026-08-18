package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapWebDtos.MapRequest;
import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapWebDtos.MapResponse;
import com.campaignorganizer.worldbuilding.application.map.port.in.CreateMapUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.DeleteMapUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.GetMapUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.ListMapsUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.UpdateMapUseCase;
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
@RequestMapping("/api/worlds/{worldId}/maps")
public class MapController {

    private final CreateMapUseCase createUseCase;
    private final UpdateMapUseCase updateUseCase;
    private final DeleteMapUseCase deleteUseCase;
    private final GetMapUseCase getUseCase;
    private final ListMapsUseCase listUseCase;
    private final MapWebMapper mapper;

    public MapController(CreateMapUseCase createUseCase, UpdateMapUseCase updateUseCase,
                         DeleteMapUseCase deleteUseCase, GetMapUseCase getUseCase,
                         ListMapsUseCase listUseCase, MapWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MapResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{mapId}")
    public MapResponse get(@PathVariable UUID worldId, @PathVariable UUID mapId) {
        return mapper.toResponse(getUseCase.get(worldId, mapId));
    }

    @PostMapping
    public ResponseEntity<MapResponse> create(@PathVariable UUID worldId,
                                              @Valid @RequestBody MapRequest request) {
        MapResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/maps/" + response.id()))
                .body(response);
    }

    @PutMapping("/{mapId}")
    public MapResponse update(@PathVariable UUID worldId, @PathVariable UUID mapId,
                              @Valid @RequestBody MapRequest request) {
        return mapper.toResponse(updateUseCase.update(mapper.toUpdateCommand(worldId, mapId, request)));
    }

    @DeleteMapping("/{mapId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID mapId) {
        deleteUseCase.delete(worldId, mapId);
    }
}
