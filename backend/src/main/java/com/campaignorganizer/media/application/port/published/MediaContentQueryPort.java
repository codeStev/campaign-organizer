package com.campaignorganizer.media.application.port.published;

import java.util.Optional;
import java.util.UUID;

/**
 * Published port: lets other bounded contexts read a media asset's bytes,
 * scoped to a world the caller already has access to. Unlike the in-port
 * {@code LoadMediaContentUseCase} (backs the public, unauthenticated
 * {@code /api/media/{id}/content} endpoint, which bypasses RLS by design),
 * this port does an ordinary world-scoped lookup — same trust level as
 * {@link MediaLookupPort#existsInWorld}.
 */
public interface MediaContentQueryPort {

    Optional<MediaContentView> loadInWorld(UUID mediaId, UUID worldId);

    record MediaContentView(String filename, String contentType, byte[] bytes) {
    }
}
