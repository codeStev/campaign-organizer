package com.campaignorganizer.tables.adapter.carddeck.in.web;

import com.campaignorganizer.tables.adapter.carddeck.in.web.CardDeckWebDtos.CardDeckRequest;
import com.campaignorganizer.tables.adapter.carddeck.in.web.CardDeckWebDtos.CardDeckResponse;
import com.campaignorganizer.tables.adapter.carddeck.in.web.CardDeckWebDtos.CardDto;
import com.campaignorganizer.tables.adapter.carddeck.in.web.CardDeckWebDtos.CardResponse;
import com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.CreateCardDeckCommand;
import com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.UpdateCardDeckCommand;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.carddeck.port.published.DeckCardView;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps card-deck web DTOs ↔ commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface CardDeckWebMapper {

    CardDeckResponse toResponse(CardDeckView view);

    CardResponse toCardResponse(DeckCardView view);

    com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.CardInput toCardInput(CardDto dto);

    List<com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.CardInput> toCardInputs(List<CardDto> cards);

    default CreateCardDeckCommand toCreateCommand(UUID worldId, CardDeckRequest request) {
        return new CreateCardDeckCommand(worldId, request.title(), request.description(),
                toCardInputs(request.cards()));
    }

    default UpdateCardDeckCommand toUpdateCommand(UUID worldId, UUID deckId, CardDeckRequest request) {
        return new UpdateCardDeckCommand(worldId, deckId, request.title(), request.description(),
                toCardInputs(request.cards()));
    }
}
