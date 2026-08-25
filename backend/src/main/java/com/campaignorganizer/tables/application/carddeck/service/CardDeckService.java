package com.campaignorganizer.tables.application.carddeck.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.CreateCardDeckCommand;
import com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.CardInput;
import com.campaignorganizer.tables.application.carddeck.port.in.CardDeckCommands.UpdateCardDeckCommand;
import com.campaignorganizer.tables.application.carddeck.port.in.CreateCardDeckUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.DeleteCardDeckUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.GetCardDeckUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.ListCardDecksUseCase;
import com.campaignorganizer.tables.application.carddeck.port.in.UpdateCardDeckUseCase;
import com.campaignorganizer.tables.application.carddeck.port.out.CardDeckRepositoryPort;
import com.campaignorganizer.tables.application.carddeck.port.out.WorldExistsPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckImportPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.carddeck.port.published.DeckCardView;
import com.campaignorganizer.tables.domain.carddeck.CardDeck;
import com.campaignorganizer.tables.domain.carddeck.DeckCard;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Card-deck use cases; also implements the published query/import ports. */
@Service
public class CardDeckService implements CreateCardDeckUseCase, UpdateCardDeckUseCase,
        DeleteCardDeckUseCase, ListCardDecksUseCase, GetCardDeckUseCase,
        CardDeckQueryPort, CardDeckImportPort {

    private final CardDeckRepositoryPort decks;
    private final WorldExistsPort worlds;
    private final CardDeckViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public CardDeckService(CardDeckRepositoryPort decks, WorldExistsPort worlds,
                           CardDeckViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.decks = decks;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CardDeckView create(CreateCardDeckCommand command) {
        requireWorld(command.worldId());
        CardDeck created = CardDeck.create(ids.newId(), command.worldId(), command.title(),
                command.description(), toCards(command.cards()), clock.instant());
        return viewMapper.toView(decks.save(created));
    }

    @Override
    @Transactional
    public CardDeckView update(UpdateCardDeckCommand command) {
        CardDeck deck = require(command.worldId(), command.deckId());
        deck.update(command.title(), command.description(), toCards(command.cards()), clock.instant());
        return viewMapper.toView(decks.save(deck));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID deckId) {
        decks.delete(require(worldId, deckId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardDeckView> list(UUID worldId) {
        requireWorld(worldId);
        return decks.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardDeckView get(UUID worldId, UUID deckId) {
        return viewMapper.toView(require(worldId, deckId));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID deckId, UUID worldId) {
        return decks.existsInWorld(deckId, worldId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CardDeckView> findByIdInWorld(UUID deckId, UUID worldId) {
        return decks.findByIdAndWorld(deckId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CardDeckView> findById(UUID deckId) {
        return decks.findById(deckId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardDeckView> findByWorld(UUID worldId) {
        return decks.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public CardDeckView importCardDeck(CardDeckView view) {
        List<DeckCard> cards = view.cards() == null ? List.of()
                : view.cards().stream().map(c -> new DeckCard(c.id(), c.title(), c.body())).toList();
        CardDeck deck = CardDeck.reconstitute(view.id(), view.worldId(), view.title(),
                view.description(), cards, view.createdAt(), view.updatedAt());
        return viewMapper.toView(decks.save(deck));
    }

    private List<DeckCard> toCards(List<CardInput> inputs) {
        return inputs == null ? List.of()
                : inputs.stream().map(c -> new DeckCard(ids.newId(), c.title(), c.body())).toList();
    }

    private CardDeck require(UUID worldId, UUID deckId) {
        return decks.findByIdAndWorld(deckId, worldId)
                .orElseThrow(() -> new NotFoundException("Card deck not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
