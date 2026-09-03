package com.campaignorganizer.tables.domain.carddeck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardDeckTest {

    private static final Instant NOW = Instant.parse("2026-08-25T10:00:00Z");

    private static DeckCard card(String title, String body) {
        return new DeckCard(UUID.randomUUID(), title, body);
    }

    @Test
    void createKeepsCardOrder() {
        DeckCard first = card("The Fool", "A new beginning. See [[Portals]]");
        DeckCard second = card("The Tower", "Disaster strikes");
        CardDeck deck = CardDeck.create(UUID.randomUUID(), UUID.randomUUID(), null, "Omens",
                "Draw for foreshadowing", List.of(first, second), NOW);

        assertThat(deck.getCards()).containsExactly(first, second);
        assertThat(deck.getTitle()).isEqualTo("Omens");
    }

    @Test
    void nullCardsBecomeEmptyList() {
        CardDeck deck = CardDeck.create(UUID.randomUUID(), UUID.randomUUID(), null, "Empty", null, null,
                NOW);
        assertThat(deck.getCards()).isEmpty();
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> CardDeck.create(UUID.randomUUID(), UUID.randomUUID(), null, " ", null,
                List.of(), NOW)).isInstanceOf(ValidationException.class);
    }

    @Test
    void updateReplacesCardsAndStampsTime() {
        CardDeck deck = CardDeck.create(UUID.randomUUID(), UUID.randomUUID(), null, "Before", null,
                List.of(card("Old", "body")), NOW);
        Instant later = NOW.plusSeconds(30);

        deck.update(null, "After", "desc", List.of(card("New A", "a"), card("New B", "b")), later);

        assertThat(deck.getTitle()).isEqualTo("After");
        assertThat(deck.getCards()).hasSize(2);
        assertThat(deck.getUpdatedAt()).isEqualTo(later);
    }
}
