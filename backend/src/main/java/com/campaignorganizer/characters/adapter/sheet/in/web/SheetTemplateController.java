package com.campaignorganizer.characters.adapter.sheet.in.web;

import com.campaignorganizer.characters.adapter.sheet.in.web.SheetTemplateWebDtos.SheetTemplateRequest;
import com.campaignorganizer.characters.adapter.sheet.in.web.SheetTemplateWebDtos.SheetTemplateResponse;
import com.campaignorganizer.characters.application.sheet.port.in.CreateSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.DeleteSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.GetSheetTemplateUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.ListSheetTemplatesUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.UpdateSheetTemplateUseCase;
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
@RequestMapping("/api/worlds/{worldId}/sheet-templates")
public class SheetTemplateController {

    private final CreateSheetTemplateUseCase createUseCase;
    private final UpdateSheetTemplateUseCase updateUseCase;
    private final DeleteSheetTemplateUseCase deleteUseCase;
    private final GetSheetTemplateUseCase getUseCase;
    private final ListSheetTemplatesUseCase listUseCase;
    private final SheetTemplateWebMapper mapper;

    public SheetTemplateController(CreateSheetTemplateUseCase createUseCase,
                                   UpdateSheetTemplateUseCase updateUseCase,
                                   DeleteSheetTemplateUseCase deleteUseCase,
                                   GetSheetTemplateUseCase getUseCase,
                                   ListSheetTemplatesUseCase listUseCase, SheetTemplateWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SheetTemplateResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{templateId}")
    public SheetTemplateResponse get(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        return mapper.toResponse(getUseCase.get(worldId, templateId));
    }

    @PostMapping
    public ResponseEntity<SheetTemplateResponse> create(@PathVariable UUID worldId,
                                                        @Valid @RequestBody SheetTemplateRequest request) {
        SheetTemplateResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/sheet-templates/" + response.id()))
                .body(response);
    }

    @PutMapping("/{templateId}")
    public SheetTemplateResponse update(@PathVariable UUID worldId, @PathVariable UUID templateId,
                                        @Valid @RequestBody SheetTemplateRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, templateId, request)));
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID templateId) {
        deleteUseCase.delete(worldId, templateId);
    }
}
