package com.campaignorganizer.handouts.adapter.in.web;

import com.campaignorganizer.handouts.adapter.in.web.HandoutCategoryWebDtos.HandoutCategoryRequest;
import com.campaignorganizer.handouts.adapter.in.web.HandoutCategoryWebDtos.HandoutCategoryResponse;
import com.campaignorganizer.handouts.application.port.in.CreateHandoutCategoryUseCase;
import com.campaignorganizer.handouts.application.port.in.DeleteHandoutCategoryUseCase;
import com.campaignorganizer.handouts.application.port.in.ListHandoutCategoriesUseCase;
import com.campaignorganizer.handouts.application.port.in.UpdateHandoutCategoryUseCase;
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
@RequestMapping("/api/worlds/{worldId}/handout-categories")
public class HandoutCategoryController {

    private final CreateHandoutCategoryUseCase createUseCase;
    private final UpdateHandoutCategoryUseCase updateUseCase;
    private final DeleteHandoutCategoryUseCase deleteUseCase;
    private final ListHandoutCategoriesUseCase listUseCase;
    private final HandoutCategoryWebMapper mapper;

    public HandoutCategoryController(CreateHandoutCategoryUseCase createUseCase,
                             UpdateHandoutCategoryUseCase updateUseCase,
                             DeleteHandoutCategoryUseCase deleteUseCase, ListHandoutCategoriesUseCase listUseCase,
                             HandoutCategoryWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<HandoutCategoryResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<HandoutCategoryResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody HandoutCategoryRequest request) {
        HandoutCategoryResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/handout-categories/" + response.id()))
                .body(response);
    }

    @PutMapping("/{categoryId}")
    public HandoutCategoryResponse update(@PathVariable UUID worldId, @PathVariable UUID categoryId,
                                   @Valid @RequestBody HandoutCategoryRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, categoryId, request)));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID categoryId) {
        deleteUseCase.delete(worldId, categoryId);
    }
}
