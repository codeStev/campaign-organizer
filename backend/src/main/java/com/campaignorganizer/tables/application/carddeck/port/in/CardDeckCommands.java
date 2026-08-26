package com.campaignorganizer.tables.application.carddeck.port.in;

import java.util.List;
import java.util.UUID;

public final class CardDeckCommands {

    private CardDeckCommands() {
    }

    public record CardInput(String title, String body,
                            List<UUID> nestedTableIds, List<UUID> nestedDeckIds) {
    }

    public record CreateCardDeckCommand(UUID worldId, String title, String description,
                                        List<CardInput> cards) {
    }

    public record UpdateCardDeckCommand(UUID worldId, UUID deckId, String title, String description,
                                        List<CardInput> cards) {
    }
}
