package com.campaignorganizer.tables.adapter.rolltable.in.web;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import com.campaignorganizer.tables.adapter.rolltable.in.web.RollTableWebDtos.RollTableRequest;
import com.campaignorganizer.tables.adapter.rolltable.in.web.RollTableWebDtos.RollTableResponse;
import com.campaignorganizer.tables.application.rolltable.port.in.CreateRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.DeleteRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.DuplicateRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.GetRollTableUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.ListRollTablesUseCase;
import com.campaignorganizer.tables.application.rolltable.port.in.UpdateRollTableUseCase;
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

/** Thin web adapter for roll tables. */
@RestController
@RequestMapping("/api/worlds/{worldId}/roll-tables")
public class RollTableController {

    private final CreateRollTableUseCase createUseCase;
    private final UpdateRollTableUseCase updateUseCase;
    private final DeleteRollTableUseCase deleteUseCase;
    private final ListRollTablesUseCase listUseCase;
    private final GetRollTableUseCase getUseCase;
    private final DuplicateRollTableUseCase duplicateUseCase;
    private final RollTableWebMapper mapper;

    public RollTableController(CreateRollTableUseCase createUseCase,
                               UpdateRollTableUseCase updateUseCase,
                               DeleteRollTableUseCase deleteUseCase,
                               ListRollTablesUseCase listUseCase, GetRollTableUseCase getUseCase,
                               DuplicateRollTableUseCase duplicateUseCase, RollTableWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.duplicateUseCase = duplicateUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<RollTableResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{tableId}")
    public RollTableResponse get(@PathVariable UUID worldId, @PathVariable UUID tableId) {
        return mapper.toResponse(getUseCase.get(worldId, tableId));
    }

    @PostMapping
    public ResponseEntity<RollTableResponse> create(@PathVariable UUID worldId,
                                                    @Valid @RequestBody RollTableRequest request) {
        RollTableResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/roll-tables/" + response.id()))
                .body(response);
    }

    @PutMapping("/{tableId}")
    public RollTableResponse update(@PathVariable UUID worldId, @PathVariable UUID tableId,
                                    @Valid @RequestBody RollTableRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, tableId, request)));
    }

    @DeleteMapping("/{tableId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID tableId) {
        deleteUseCase.delete(worldId, tableId);
    }

    @PostMapping("/{tableId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public RollTableResponse duplicate(@PathVariable UUID worldId, @PathVariable UUID tableId) {
        return mapper.toResponse(duplicateUseCase.duplicate(worldId, tableId));
    }
}
