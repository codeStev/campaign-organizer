package com.campaignorganizer.campaign.adapter.session.out.context;

import com.campaignorganizer.campaign.application.session.port.out.CharacterSheetExistsPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * ACL: resolves character-sheet existence/name for the session attendance
 * module via the characters context's published query port (ADR-0091). A
 * sheet counts as usable for a campaign if it's either shared (no
 * campaignId) or scoped to that exact campaign.
 */
@Component
public class SessionCharacterSheetExistsAdapter implements CharacterSheetExistsPort {

    private final CharacterSheetQueryPort characterSheets;

    public SessionCharacterSheetExistsAdapter(CharacterSheetQueryPort characterSheets) {
        this.characterSheets = characterSheets;
    }

    @Override
    public boolean existsForCampaign(UUID characterId, UUID worldId, UUID campaignId) {
        return characterSheets.findByIdInWorld(characterId, worldId)
                .map(view -> view.campaignId() == null || view.campaignId().equals(campaignId))
                .orElse(false);
    }

    @Override
    public Optional<String> findName(UUID characterId, UUID worldId) {
        return characterSheets.findByIdInWorld(characterId, worldId).map(CharacterSheetView::name);
    }
}
