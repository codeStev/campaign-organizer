package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.adapter.template.in.web.GlobalFieldTemplateWebDtos.GlobalFieldTemplateRequest;
import com.campaignorganizer.characters.adapter.template.in.web.GlobalFieldTemplateWebDtos.GlobalFieldTemplateResponse;
import com.campaignorganizer.characters.application.template.port.in.CreateGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.DeleteGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.GetGlobalFieldTemplateUseCase;
import com.campaignorganizer.characters.application.template.port.in.ListGlobalFieldTemplatesUseCase;
import com.campaignorganizer.characters.application.template.port.in.UpdateGlobalFieldTemplateUseCase;
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

/** World-independent, system-scoped field template catalog (ADR-0093). */
@RestController
@RequestMapping("/api/field-templates/global")
public class GlobalFieldTemplateController {

    private final CreateGlobalFieldTemplateUseCase createUseCase;
    private final UpdateGlobalFieldTemplateUseCase updateUseCase;
    private final DeleteGlobalFieldTemplateUseCase deleteUseCase;
    private final GetGlobalFieldTemplateUseCase getUseCase;
    private final ListGlobalFieldTemplatesUseCase listUseCase;
    private final GlobalFieldTemplateWebMapper mapper;

    public GlobalFieldTemplateController(CreateGlobalFieldTemplateUseCase createUseCase,
                                         UpdateGlobalFieldTemplateUseCase updateUseCase,
                                         DeleteGlobalFieldTemplateUseCase deleteUseCase,
                                         GetGlobalFieldTemplateUseCase getUseCase,
                                         ListGlobalFieldTemplatesUseCase listUseCase,
                                         GlobalFieldTemplateWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<GlobalFieldTemplateResponse> list(@RequestParam(required = false) TemplateKind kind) {
        return listUseCase.list(kind).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{templateId}")
    public GlobalFieldTemplateResponse get(@PathVariable UUID templateId) {
        return mapper.toResponse(getUseCase.get(templateId));
    }

    @PostMapping
    public ResponseEntity<GlobalFieldTemplateResponse> create(
            @Valid @RequestBody GlobalFieldTemplateRequest request) {
        GlobalFieldTemplateResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(request)));
        return ResponseEntity
                .created(URI.create("/api/field-templates/global/" + response.id()))
                .body(response);
    }

    @PutMapping("/{templateId}")
    public GlobalFieldTemplateResponse update(@PathVariable UUID templateId,
                                              @Valid @RequestBody GlobalFieldTemplateRequest request) {
        return mapper.toResponse(updateUseCase.update(mapper.toUpdateCommand(templateId, request)));
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID templateId) {
        deleteUseCase.delete(templateId);
    }
}
