package com.campaignorganizer.characters.application.document.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read documents from other contexts (export). */
public interface DocumentQueryPort {

    List<DocumentView> findByWorld(UUID worldId);
}
