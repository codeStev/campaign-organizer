package com.campaignorganizer.campaign.domain.session;

import com.campaignorganizer.shared.domain.ValidationException;
import java.util.UUID;

/**
 * One condensed entry on a session cheat sheet (FR-37): either freeform text
 * or a reference to existing content — a statblock, one roll-table row, or
 * one deck card — so the sheet stays a one-page summary instead of a copy.
 */
public record CheatSheetFragment(UUID id, Type type, String text, UUID statblockId,
                                 UUID tableId, UUID entryId, UUID deckId, UUID cardId) {

    public enum Type {
        FREEFORM, STATBLOCK, TABLE_ROW, DECK_CARD
    }

    public CheatSheetFragment {
        switch (type) {
            case FREEFORM -> requireText(text);
            case STATBLOCK -> require(statblockId, "statblock");
            case TABLE_ROW -> {
                require(tableId, "roll table");
                require(entryId, "table entry");
            }
            case DECK_CARD -> {
                require(deckId, "card deck");
                require(cardId, "deck card");
            }
        }
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Freeform cheat-sheet fragment needs text");
        }
    }

    private static void require(UUID id, String what) {
        if (id == null) {
            throw new ValidationException("Cheat-sheet fragment references no " + what);
        }
    }
}
