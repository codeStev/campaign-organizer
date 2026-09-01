package com.campaignorganizer.characters.application.document.port.in;

import java.util.Map;
import java.util.UUID;

public final class DocumentCommands {

    private DocumentCommands() {
    }

    public record CreateDocumentCommand(UUID worldId, UUID templateId, UUID campaignId, String name,
                                         Map<String, Object> values) {
    }

    public record UpdateDocumentCommand(UUID worldId, UUID documentId, UUID templateId, UUID campaignId,
                                         String name, Map<String, Object> values) {
    }
}
