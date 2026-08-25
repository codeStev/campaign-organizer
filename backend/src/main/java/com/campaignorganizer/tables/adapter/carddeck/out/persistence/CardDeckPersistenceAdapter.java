package com.campaignorganizer.tables.adapter.carddeck.out.persistence;

import com.campaignorganizer.tables.application.carddeck.port.out.CardDeckRepositoryPort;
import com.campaignorganizer.tables.domain.carddeck.CardDeck;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the card-deck repository port. */
@Component
public class CardDeckPersistenceAdapter implements CardDeckRepositoryPort {

    private final CardDeckJpaRepository repository;
    private final CardDeckPersistenceMapper mapper;

    public CardDeckPersistenceAdapter(CardDeckJpaRepository repository,
                                      CardDeckPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CardDeck> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<CardDeck> findByIdAndWorld(UUID deckId, UUID worldId) {
        return repository.findByIdAndWorldId(deckId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<CardDeck> findById(UUID deckId) {
        return repository.findById(deckId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID deckId, UUID worldId) {
        return repository.existsByIdAndWorldId(deckId, worldId);
    }

    @Override
    public CardDeck save(CardDeck deck) {
        return mapper.toDomain(repository.save(mapper.toEntity(deck)));
    }

    @Override
    public void delete(CardDeck deck) {
        repository.deleteById(deck.getId());
    }
}
