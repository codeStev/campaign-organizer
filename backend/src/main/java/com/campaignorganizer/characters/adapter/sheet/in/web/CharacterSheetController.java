package com.campaignorganizer.characters.adapter.sheet.in.web;

import com.campaignorganizer.characters.adapter.sheet.in.web.CharacterSheetWebDtos.CharacterSheetRequest;
import com.campaignorganizer.characters.adapter.sheet.in.web.CharacterSheetWebDtos.CharacterSheetResponse;
import com.campaignorganizer.characters.application.sheet.port.in.CreateCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.DeleteCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.GetCharacterSheetUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.ListCharacterSheetsUseCase;
import com.campaignorganizer.characters.application.sheet.port.in.UpdateCharacterSheetUseCase;
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
@RequestMapping("/api/worlds/{worldId}/character-sheets")
public class CharacterSheetController {

    private final CreateCharacterSheetUseCase createUseCase;
    private final UpdateCharacterSheetUseCase updateUseCase;
    private final DeleteCharacterSheetUseCase deleteUseCase;
    private final GetCharacterSheetUseCase getUseCase;
    private final ListCharacterSheetsUseCase listUseCase;
    private final CharacterSheetWebMapper mapper;

    public CharacterSheetController(CreateCharacterSheetUseCase createUseCase,
                                    UpdateCharacterSheetUseCase updateUseCase,
                                    DeleteCharacterSheetUseCase deleteUseCase,
                                    GetCharacterSheetUseCase getUseCase,
                                    ListCharacterSheetsUseCase listUseCase,
                                    CharacterSheetWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CharacterSheetResponse> list(@PathVariable UUID worldId,
                                             @RequestParam(required = false) UUID campaignId) {
        return listUseCase.list(worldId, campaignId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{sheetId}")
    public CharacterSheetResponse get(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        return mapper.toResponse(getUseCase.get(worldId, sheetId));
    }

    @PostMapping
    public ResponseEntity<CharacterSheetResponse> create(@PathVariable UUID worldId,
                                                         @Valid @RequestBody CharacterSheetRequest request) {
        CharacterSheetResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/character-sheets/" + response.id()))
                .body(response);
    }

    @PutMapping("/{sheetId}")
    public CharacterSheetResponse update(@PathVariable UUID worldId, @PathVariable UUID sheetId,
                                         @Valid @RequestBody CharacterSheetRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, sheetId, request)));
    }

    @DeleteMapping("/{sheetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID sheetId) {
        deleteUseCase.delete(worldId, sheetId);
    }
}
