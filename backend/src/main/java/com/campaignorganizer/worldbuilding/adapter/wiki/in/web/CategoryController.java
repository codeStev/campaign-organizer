package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.CategoryWebDtos.CategoryRequest;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.CategoryWebDtos.CategoryResponse;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CreateCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.DeleteCategoryUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ListCategoriesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.UpdateCategoryUseCase;
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
@RequestMapping("/api/worlds/{worldId}/categories")
public class CategoryController {

    private final CreateCategoryUseCase createUseCase;
    private final UpdateCategoryUseCase updateUseCase;
    private final DeleteCategoryUseCase deleteUseCase;
    private final ListCategoriesUseCase listUseCase;
    private final CategoryWebMapper mapper;

    public CategoryController(CreateCategoryUseCase createUseCase, UpdateCategoryUseCase updateUseCase,
                             DeleteCategoryUseCase deleteUseCase, ListCategoriesUseCase listUseCase,
                             CategoryWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CategoryResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/categories/" + response.id()))
                .body(response);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse update(@PathVariable UUID worldId, @PathVariable UUID categoryId,
                                   @Valid @RequestBody CategoryRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, categoryId, request)));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID categoryId) {
        deleteUseCase.delete(worldId, categoryId);
    }
}
