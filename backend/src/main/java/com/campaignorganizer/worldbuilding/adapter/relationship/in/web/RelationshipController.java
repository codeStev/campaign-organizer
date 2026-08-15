package com.campaignorganizer.worldbuilding.adapter.relationship.in.web;

import com.campaignorganizer.worldbuilding.adapter.relationship.in.web.RelationshipWebDtos.RelationshipRequest;
import com.campaignorganizer.worldbuilding.adapter.relationship.in.web.RelationshipWebDtos.RelationshipResponse;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.CreateRelationshipUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.DeleteRelationshipUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.ListRelationshipsUseCase;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.UpdateRelationshipUseCase;
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

/** Thin web adapter for relationship CRUD. */
@RestController
@RequestMapping("/api/worlds/{worldId}/relationships")
public class RelationshipController {

    private final CreateRelationshipUseCase createUseCase;
    private final UpdateRelationshipUseCase updateUseCase;
    private final DeleteRelationshipUseCase deleteUseCase;
    private final ListRelationshipsUseCase listUseCase;
    private final RelationshipWebMapper mapper;

    public RelationshipController(CreateRelationshipUseCase createUseCase,
                                 UpdateRelationshipUseCase updateUseCase,
                                 DeleteRelationshipUseCase deleteUseCase,
                                 ListRelationshipsUseCase listUseCase, RelationshipWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<RelationshipResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<RelationshipResponse> create(@PathVariable UUID worldId,
                                                       @Valid @RequestBody RelationshipRequest request) {
        RelationshipResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/relationships/" + response.id()))
                .body(response);
    }

    @PutMapping("/{relationshipId}")
    public RelationshipResponse update(@PathVariable UUID worldId, @PathVariable UUID relationshipId,
                                       @Valid @RequestBody RelationshipRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, relationshipId, request)));
    }

    @DeleteMapping("/{relationshipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID relationshipId) {
        deleteUseCase.delete(worldId, relationshipId);
    }
}
