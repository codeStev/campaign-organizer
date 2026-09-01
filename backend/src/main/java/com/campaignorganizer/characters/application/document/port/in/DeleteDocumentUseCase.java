package com.campaignorganizer.characters.application.document.port.in;

import java.util.UUID;

public interface DeleteDocumentUseCase {

    void delete(UUID worldId, UUID documentId);
}
