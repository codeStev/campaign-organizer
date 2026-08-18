package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapPinWebDtos.MapPinRequest;
import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapPinWebDtos.MapPinResponse;
import com.campaignorganizer.worldbuilding.application.map.port.in.CreateMapPinUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.DeleteMapPinUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.ListMapPinsUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.UpdateMapPinUseCase;
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
@RequestMapping("/api/worlds/{worldId}/maps/{mapId}/pins")
public class MapPinController {

    private final CreateMapPinUseCase createUseCase;
    private final UpdateMapPinUseCase updateUseCase;
    private final DeleteMapPinUseCase deleteUseCase;
    private final ListMapPinsUseCase listUseCase;
    private final MapPinWebMapper mapper;

    public MapPinController(CreateMapPinUseCase createUseCase, UpdateMapPinUseCase updateUseCase,
                            DeleteMapPinUseCase deleteUseCase, ListMapPinsUseCase listUseCase,
                            MapPinWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MapPinResponse> list(@PathVariable UUID worldId, @PathVariable UUID mapId) {
        return listUseCase.list(worldId, mapId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<MapPinResponse> create(@PathVariable UUID worldId, @PathVariable UUID mapId,
                                                 @Valid @RequestBody MapPinRequest request) {
        MapPinResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, mapId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/maps/" + mapId + "/pins/" + response.id()))
                .body(response);
    }

    @PutMapping("/{pinId}")
    public MapPinResponse update(@PathVariable UUID worldId, @PathVariable UUID mapId,
                                 @PathVariable UUID pinId, @Valid @RequestBody MapPinRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, mapId, pinId, request)));
    }

    @DeleteMapping("/{pinId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID mapId, @PathVariable UUID pinId) {
        deleteUseCase.delete(worldId, mapId, pinId);
    }
}
