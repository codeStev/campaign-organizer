package com.campaignorganizer.tables.adapter.carddeck.out.persistence;

import com.campaignorganizer.tables.domain.carddeck.CardDeck;
import com.campaignorganizer.tables.domain.carddeck.DeckCard;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface CardDeckPersistenceMapper {

    CardDeckJpaEntity toEntity(CardDeck deck);

    DeckCardJson toJson(DeckCard card);

    DeckCard toCard(DeckCardJson json);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default CardDeck toDomain(CardDeckJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return CardDeck.reconstitute(
                entity.getId(),
                entity.getWorldId(),
                entity.getCategoryId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCards().stream().map(this::toCard).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
