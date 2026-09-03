package com.campaignorganizer.characters.adapter.category.in.web;

import com.campaignorganizer.characters.adapter.category.in.web.SheetCategoryWebDtos.SheetCategoryRequest;
import com.campaignorganizer.characters.adapter.category.in.web.SheetCategoryWebDtos.SheetCategoryResponse;
import com.campaignorganizer.characters.application.category.port.in.CreateSheetCategoryUseCase;
import com.campaignorganizer.characters.application.category.port.in.DeleteSheetCategoryUseCase;
import com.campaignorganizer.characters.application.category.port.in.ListSheetCategoriesUseCase;
import com.campaignorganizer.characters.application.category.port.in.UpdateSheetCategoryUseCase;
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
@RequestMapping("/api/worlds/{worldId}/sheet-categories")
public class SheetCategoryController {

    private final CreateSheetCategoryUseCase createUseCase;
    private final UpdateSheetCategoryUseCase updateUseCase;
    private final DeleteSheetCategoryUseCase deleteUseCase;
    private final ListSheetCategoriesUseCase listUseCase;
    private final SheetCategoryWebMapper mapper;

    public SheetCategoryController(CreateSheetCategoryUseCase createUseCase,
                             UpdateSheetCategoryUseCase updateUseCase,
                             DeleteSheetCategoryUseCase deleteUseCase, ListSheetCategoriesUseCase listUseCase,
                             SheetCategoryWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SheetCategoryResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<SheetCategoryResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody SheetCategoryRequest request) {
        SheetCategoryResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/sheet-categories/" + response.id()))
                .body(response);
    }

    @PutMapping("/{categoryId}")
    public SheetCategoryResponse update(@PathVariable UUID worldId, @PathVariable UUID categoryId,
                                   @Valid @RequestBody SheetCategoryRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, categoryId, request)));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID categoryId) {
        deleteUseCase.delete(worldId, categoryId);
    }
}
