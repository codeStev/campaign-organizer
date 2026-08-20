package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.adapter.template.in.web.FieldTemplateWebDtos.FieldTemplateRequest;
import com.campaignorganizer.characters.adapter.template.in.web.FieldTemplateWebDtos.FieldTemplateResponse;
import com.campaignorganizer.characters.application.template.port.in.CreateFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.DeleteFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.GetFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.ListFieldTemplatesUseCase;
import com.campaignorganizer.characters.application.template.port.in.UpdateFieldTemplateUseCase;
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
@RequestMapping("/api/worlds/{worldId}/field-templates")
public class FieldTemplateController {

    private final CreateFieldTemplateUseCase createUseCase;
    private final UpdateFieldTemplateUseCase updateUseCase;
    private final DeleteFieldTemplateUseCase deleteUseCase;
    private final GetFieldTemplateUseCase getUseCase;
    private final ListFieldTemplatesUseCase listUseCase;
    private final FieldTemplateWebMapper mapper;

    public FieldTemplateController(CreateFieldTemplateUseCase createUseCase,
                                   UpdateFieldTemplateUseCase updateUseCase,
                                   DeleteFieldTemplateUseCase deleteUseCase,
                                   GetFieldTemplateUseCase getUseCase,
                                   ListFieldTemplatesUseCase listUseCase, FieldTemplateWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<FieldTemplateResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{templateId}")
    public FieldTemplateResponse get(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        return mapper.toResponse(getUseCase.get(worldId, templateId));
    }

    @PostMapping
    public ResponseEntity<FieldTemplateResponse> create(@PathVariable UUID worldId,
                                                        @Valid @RequestBody FieldTemplateRequest request) {
        FieldTemplateResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/field-templates/" + response.id()))
                .body(response);
    }

    @PutMapping("/{templateId}")
    public FieldTemplateResponse update(@PathVariable UUID worldId, @PathVariable UUID templateId,
                                        @Valid @RequestBody FieldTemplateRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, templateId, request)));
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        deleteUseCase.delete(worldId, templateId);
    }
}
