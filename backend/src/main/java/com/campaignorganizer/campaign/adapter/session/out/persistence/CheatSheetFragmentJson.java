package com.campaignorganizer.campaign.adapter.session.out.persistence;

/**
 * JSON-serialisable fragment stored in the cheat_sheets.fragments jsonb
 * column; mirrors the domain record field-for-field with the type as name.
 */
public record CheatSheetFragmentJson(String id, String type, String text, String statblockId,
                                     String tableId, String entryId, String deckId,
                                     String cardId) {
}
