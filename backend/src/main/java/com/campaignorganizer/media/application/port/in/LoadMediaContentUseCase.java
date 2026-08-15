package com.campaignorganizer.media.application.port.in;

import java.util.UUID;

/** Load an asset's bytes for the public content endpoint. */
public interface LoadMediaContentUseCase {

    MediaContent load(UUID mediaId);

    record MediaContent(String contentType, byte[] bytes) {
    }
}
