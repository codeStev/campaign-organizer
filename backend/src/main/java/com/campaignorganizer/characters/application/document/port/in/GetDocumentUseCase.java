package com.campaignorganizer.characters.application.document.port.in;

import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import java.util.UUID;

public interface GetDocumentUseCase {

    DocumentView get(UUID worldId, UUID documentId);
}
