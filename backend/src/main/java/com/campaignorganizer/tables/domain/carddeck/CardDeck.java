package com.campaignorganizer.tables.domain.carddeck;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A customized deck of cards (aggregate root, FR-40): print-first — cards print
 * as cut-out reference cards — with a stateless digital draw (pick a random
 * card, nothing tracks what was drawn). Cards are value objects; list order is
 * the deck order.
 */
public final class CardDeck {

    private final UUID id;
    private final UUID worldId;
    private String title;
    private String description;
    private List<DeckCard> cards;
    private final Instant createdAt;
    private Instant updatedAt;

    private CardDeck(UUID id, UUID worldId, String title, String description, List<DeckCard> cards,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(title, description, cards);
    }

    public static CardDeck create(UUID id, UUID worldId, String title, String description,
                                  List<DeckCard> cards, Instant now) {
        return new CardDeck(id, worldId, title, description, cards, now, now);
    }

    public static CardDeck reconstitute(UUID id, UUID worldId, String title, String description,
                                        List<DeckCard> cards, Instant createdAt, Instant updatedAt) {
        return new CardDeck(id, worldId, title, description, cards, createdAt, updatedAt);
    }

    public void update(String title, String description, List<DeckCard> cards, Instant now) {
        apply(title, description, cards);
        this.updatedAt = now;
    }

    private void apply(String title, String description, List<DeckCard> cards) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Card deck title must not be blank");
        }
        if (title.length() > 200) {
            throw new ValidationException("Card deck title must not exceed 200 characters");
        }
        this.title = title;
        this.description = description;
        this.cards = cards == null ? List.of() : List.copyOf(cards);
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<DeckCard> getCards() {
        return cards;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
