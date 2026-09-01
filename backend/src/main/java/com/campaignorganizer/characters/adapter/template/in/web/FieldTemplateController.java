package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.adapter.template.in.web.FieldTemplateWebDtos.FieldTemplateRequest;
import com.campaignorganizer.characters.adapter.template.in.web.FieldTemplateWebDtos.FieldTemplateResponse;
import com.campaignorganizer.characters.adapter.template.in.web.GlobalFieldTemplateWebDtos.GlobalFieldTemplateResponse;
import com.campaignorganizer.characters.application.template.port.in.CreateFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.DeleteFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.DuplicateFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.GetFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.ListFieldTemplatesUseCase;
import com.campaignorganizer.characters.application.template.port.in.PromoteFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.UpdateFieldTemplateUseCase;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    private final DuplicateFieldTemplateUseCase duplicateUseCase;
    private final PromoteFieldTemplateUseCase promoteUseCase;
    private final FieldTemplateWebMapper mapper;
    private final GlobalFieldTemplateWebMapper globalMapper;

    public FieldTemplateController(CreateFieldTemplateUseCase createUseCase,
                                   UpdateFieldTemplateUseCase updateUseCase,
                                   DeleteFieldTemplateUseCase deleteUseCase,
                                   GetFieldTemplateUseCase getUseCase,
                                   ListFieldTemplatesUseCase listUseCase,
                                   DuplicateFieldTemplateUseCase duplicateUseCase,
                                   PromoteFieldTemplateUseCase promoteUseCase,
                                   FieldTemplateWebMapper mapper, GlobalFieldTemplateWebMapper globalMapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.duplicateUseCase = duplicateUseCase;
        this.promoteUseCase = promoteUseCase;
        this.mapper = mapper;
        this.globalMapper = globalMapper;
    }

    @GetMapping
    public List<FieldTemplateResponse> list(@PathVariable UUID worldId,
                                            @RequestParam(required = false) TemplateKind kind) {
        return listUseCase.list(worldId, kind).stream().map(mapper::toResponse).toList();
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

    @PostMapping("/{templateId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public FieldTemplateResponse duplicate(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        return mapper.toResponse(duplicateUseCase.duplicate(worldId, templateId));
    }

    @PostMapping("/{templateId}/promote")
    @ResponseStatus(HttpStatus.CREATED)
    public GlobalFieldTemplateResponse promote(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        return globalMapper.toResponse(promoteUseCase.promote(worldId, templateId));
    }
}
