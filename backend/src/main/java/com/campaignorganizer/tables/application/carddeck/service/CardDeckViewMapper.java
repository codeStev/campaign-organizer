package com.campaignorganizer.tables.application.carddeck.service;

import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.carddeck.port.published.DeckCardView;
import com.campaignorganizer.tables.domain.carddeck.CardDeck;
import com.campaignorganizer.tables.domain.carddeck.DeckCard;
import org.mapstruct.Mapper;

/** Maps the domain card deck to the published read model (MapStruct). */
@Mapper(componentModel = "spring")
public interface CardDeckViewMapper {

    CardDeckView toView(CardDeck deck);

    DeckCardView toCardView(DeckCard card);
}
