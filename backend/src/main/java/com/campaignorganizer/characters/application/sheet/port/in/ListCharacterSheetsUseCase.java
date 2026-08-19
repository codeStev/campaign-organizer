package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import java.util.List;
import java.util.UUID;

public interface ListCharacterSheetsUseCase {

    /** Lists a world's sheets; when {@code campaignId} is set, scopes to that campaign (ADR-0031). */
    List<CharacterSheetView> list(UUID worldId, UUID campaignId);
}
