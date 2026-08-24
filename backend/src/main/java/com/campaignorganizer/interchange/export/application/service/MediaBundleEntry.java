package com.campaignorganizer.interchange.export.application.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Media metadata as carried in a world's export bundle (ADR-0061) — deliberately
 * not {@code media.application.port.in.MediaView}: the bundle is a JSON contract
 * between {@code backup} and this context, not a shared Java type, so the two
 * sides don't need a cross-context dependency to agree on it.
 */
record MediaBundleEntry(UUID id, UUID worldId, String filename, String contentType, long sizeBytes,
        Instant createdAt) {
}
