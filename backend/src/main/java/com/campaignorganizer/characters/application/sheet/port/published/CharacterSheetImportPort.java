package com.campaignorganizer.characters.application.sheet.port.published;

/**
 * Published port: persists a character sheet exactly as given (id and
 * foreign keys already resolved by the caller) instead of generating a new
 * id — backup import's counterpart to the normal create flow (ADR-0061).
 */
public interface CharacterSheetImportPort {

    CharacterSheetView importCharacterSheet(CharacterSheetView view);
}
