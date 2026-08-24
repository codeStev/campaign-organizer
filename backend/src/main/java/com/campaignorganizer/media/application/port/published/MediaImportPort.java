package com.campaignorganizer.media.application.port.published;

import java.time.Instant;
import java.util.UUID;

/**
 * Published port: persists a media asset's bytes and metadata exactly as
 * given (id and foreign keys already resolved by the caller) instead of
 * generating a new id — backup import's counterpart to the normal upload
 * flow (ADR-0061).
 */
public interface MediaImportPort {

    void importMedia(UUID id, UUID worldId, String filename, String contentType, byte[] content,
            Instant createdAt);
}
