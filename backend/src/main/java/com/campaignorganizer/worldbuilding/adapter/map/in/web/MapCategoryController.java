package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapCategoryWebDtos.MapCategoryRequest;
import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapCategoryWebDtos.MapCategoryResponse;
import com.campaignorganizer.worldbuilding.application.map.port.in.CreateMapCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.DeleteMapCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.ListMapCategoriesUseCase;
import com.campaignorganizer.worldbuilding.application.map.port.in.UpdateMapCategoryUseCase;
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
@RequestMapping("/api/worlds/{worldId}/map-categories")
public class MapCategoryController {

    private final CreateMapCategoryUseCase createUseCase;
    private final UpdateMapCategoryUseCase updateUseCase;
    private final DeleteMapCategoryUseCase deleteUseCase;
    private final ListMapCategoriesUseCase listUseCase;
    private final MapCategoryWebMapper mapper;

    public MapCategoryController(CreateMapCategoryUseCase createUseCase, UpdateMapCategoryUseCase updateUseCase,
                             DeleteMapCategoryUseCase deleteUseCase, ListMapCategoriesUseCase listUseCase,
                             MapCategoryWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MapCategoryResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<MapCategoryResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody MapCategoryRequest request) {
        MapCategoryResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/map-categories/" + response.id()))
                .body(response);
    }

    @PutMapping("/{categoryId}")
    public MapCategoryResponse update(@PathVariable UUID worldId, @PathVariable UUID categoryId,
                                   @Valid @RequestBody MapCategoryRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, categoryId, request)));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID categoryId) {
        deleteUseCase.delete(worldId, categoryId);
    }
}
