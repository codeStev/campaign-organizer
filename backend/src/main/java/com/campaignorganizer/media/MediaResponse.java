package com.campaignorganizer.media;

import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
        UUID id,
        UUID worldId,
        String filename,
        String contentType,
        long size,
        String url,
        Instant createdAt) {

    public static MediaResponse from(MediaAsset asset) {
        return new MediaResponse(
                asset.getId(),
                asset.getWorldId(),
                asset.getFilename(),
                asset.getContentType(),
                asset.getSizeBytes(),
                "/api/media/" + asset.getId() + "/content",
                asset.getCreatedAt());
    }
}
