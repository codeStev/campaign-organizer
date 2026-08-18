package com.campaignorganizer.media.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

/** Web response for a media asset (includes the derived content URL). */
public record MediaResponse(
        UUID id,
        UUID worldId,
        String filename,
        String contentType,
        long size,
        String url,
        Instant createdAt) {
}
