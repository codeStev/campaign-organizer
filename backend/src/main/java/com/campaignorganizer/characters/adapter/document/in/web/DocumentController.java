package com.campaignorganizer.characters.adapter.document.in.web;

import com.campaignorganizer.characters.adapter.document.in.web.DocumentWebDtos.DocumentRequest;
import com.campaignorganizer.characters.adapter.document.in.web.DocumentWebDtos.DocumentResponse;
import com.campaignorganizer.characters.application.document.port.in.CreateDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.in.DeleteDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.in.GetDocumentUseCase;
import com.campaignorganizer.characters.application.document.port.in.ListDocumentsUseCase;
import com.campaignorganizer.characters.application.document.port.in.UpdateDocumentUseCase;
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
@RequestMapping("/api/worlds/{worldId}/documents")
public class DocumentController {

    private final CreateDocumentUseCase createUseCase;
    private final UpdateDocumentUseCase updateUseCase;
    private final DeleteDocumentUseCase deleteUseCase;
    private final GetDocumentUseCase getUseCase;
    private final ListDocumentsUseCase listUseCase;
    private final DocumentWebMapper mapper;

    public DocumentController(CreateDocumentUseCase createUseCase, UpdateDocumentUseCase updateUseCase,
                              DeleteDocumentUseCase deleteUseCase, GetDocumentUseCase getUseCase,
                              ListDocumentsUseCase listUseCase, DocumentWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<DocumentResponse> list(@PathVariable UUID worldId,
                                       @RequestParam(required = false) UUID campaignId) {
        return listUseCase.list(worldId, campaignId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{documentId}")
    public DocumentResponse get(@PathVariable UUID worldId, @PathVariable UUID documentId) {
        return mapper.toResponse(getUseCase.get(worldId, documentId));
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody DocumentRequest request) {
        DocumentResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/documents/" + response.id()))
                .body(response);
    }

    @PutMapping("/{documentId}")
    public DocumentResponse update(@PathVariable UUID worldId, @PathVariable UUID documentId,
                                   @Valid @RequestBody DocumentRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, documentId, request)));
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID documentId) {
        deleteUseCase.delete(worldId, documentId);
    }
}
