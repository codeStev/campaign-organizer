package com.campaignorganizer.media.application.port.out;

import java.util.Optional;

/**
 * Binary storage for media bytes (ADR-0007). Framework-free: bytes in, bytes out —
 * the default adapter writes the local volume; an S3 adapter could replace it.
 */
public interface MediaStoragePort {

    /** Persist the bytes and return an opaque storage key. */
    String store(byte[] data);

    /** Load the bytes for {@code key}, or empty if the bytes are missing. */
    Optional<byte[]> load(String key);

    /** Remove the bytes stored under {@code key}, if present. */
    void delete(String key);
}
