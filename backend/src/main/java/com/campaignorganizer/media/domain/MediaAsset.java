package com.campaignorganizer.media.domain;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * A stored media asset (aggregate root): the metadata for an uploaded image. Owns
 * the upload rules (allowed image types, size limit). Pure domain — no framework.
 */
public final class MediaAsset {

    public static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");

    private final UUID id;
    private final UUID worldId;
    private final String filename;
    private final String contentType;
    private final long sizeBytes;
    private final String storageKey;
    private final Instant createdAt;

    private MediaAsset(UUID id, UUID worldId, String filename, String contentType, long sizeBytes,
                       String storageKey, Instant createdAt) {
        this.id = id;
        this.worldId = worldId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.createdAt = createdAt;
    }

    /** Register a freshly uploaded asset (validates the upload rules). */
    public static MediaAsset register(UUID id, UUID worldId, String filename, String contentType,
                                      long sizeBytes, String storageKey, Instant now) {
        ensureUploadable(contentType, sizeBytes);
        return new MediaAsset(id, worldId, filename, contentType, sizeBytes, storageKey, now);
    }

    /** Rebuild an existing asset from storage (trusted data). */
    public static MediaAsset reconstitute(UUID id, UUID worldId, String filename, String contentType,
                                          long sizeBytes, String storageKey, Instant createdAt) {
        return new MediaAsset(id, worldId, filename, contentType, sizeBytes, storageKey, createdAt);
    }

    /** The upload rules, callable before the bytes are stored. */
    public static void ensureUploadable(String contentType, long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new ValidationException("Empty file");
        }
        if (sizeBytes > MAX_SIZE_BYTES) {
            throw new ValidationException("File exceeds 10MB limit");
        }
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ValidationException("Only image uploads are allowed");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
