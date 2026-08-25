package com.campaignorganizer.tables.adapter.carddeck.in.web;

import com.campaignorganizer.tables.adapter.carddeck.in.web.CardDeckWebDtos.CardDeckRequest;
import com.campaignorganizer.tables.adapter.carddeck.in.web.CardDeckWebDtos.CardDeckResponse;
import com.campaignorganizer.tables.application.carddeck.port.in.CreateCardDeckUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.DeleteCardDeckUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.GetCardDeckUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.ListCardDecksUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.UpdateCardDeckUseCase;
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

/** Thin web adapter for card decks. */
@RestController
@RequestMapping("/api/worlds/{worldId}/card-decks")
public class CardDeckController {

    private final CreateCardDeckUseCase createUseCase;
    private final UpdateCardDeckUseCase updateUseCase;
    private final DeleteCardDeckUseCase deleteUseCase;
    private final ListCardDecksUseCase listUseCase;
    private final GetCardDeckUseCase getUseCase;
    private final CardDeckWebMapper mapper;

    public CardDeckController(CreateCardDeckUseCase createUseCase,
                              UpdateCardDeckUseCase updateUseCase,
                              DeleteCardDeckUseCase deleteUseCase,
                              ListCardDecksUseCase listUseCase, GetCardDeckUseCase getUseCase,
                              CardDeckWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CardDeckResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{deckId}")
    public CardDeckResponse get(@PathVariable UUID worldId, @PathVariable UUID deckId) {
        return mapper.toResponse(getUseCase.get(worldId, deckId));
    }

    @PostMapping
    public ResponseEntity<CardDeckResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody CardDeckRequest request) {
        CardDeckResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/card-decks/" + response.id()))
                .body(response);
    }

    @PutMapping("/{deckId}")
    public CardDeckResponse update(@PathVariable UUID worldId, @PathVariable UUID deckId,
                                   @Valid @RequestBody CardDeckRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, deckId, request)));
    }

    @DeleteMapping("/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID deckId) {
        deleteUseCase.delete(worldId, deckId);
    }
}
