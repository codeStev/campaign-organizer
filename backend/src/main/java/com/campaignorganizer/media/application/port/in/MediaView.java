package com.campaignorganizer.media.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** Application read model for a media asset (the content URL is a web concern). */
public record MediaView(
        UUID id,
        UUID worldId,
        String filename,
        String contentType,
        long sizeBytes,
        Instant createdAt) {
}
