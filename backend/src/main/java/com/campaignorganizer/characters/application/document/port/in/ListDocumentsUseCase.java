package com.campaignorganizer.characters.application.document.port.in;

import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import java.util.List;
import java.util.UUID;

public interface ListDocumentsUseCase {

    /** Lists a world's documents; when {@code campaignId} is set, scopes to that campaign. */
    List<DocumentView> list(UUID worldId, UUID campaignId);
}
