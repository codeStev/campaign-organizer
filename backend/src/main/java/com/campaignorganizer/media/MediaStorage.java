package com.campaignorganizer.media;

import org.springframework.core.io.Resource;

/**
 * Binary storage for media assets. The default implementation writes to the
 * local volume (ADR-0007 / ADR-0016); an S3-backed implementation could replace
 * it without changing callers.
 */
public interface MediaStorage {

    /** Persist the bytes and return an opaque storage key. */
    String store(byte[] data);

    /** Load the bytes previously stored under {@code key}. */
    Resource load(String key);

    /** Remove the bytes stored under {@code key}, if present. */
    void delete(String key);
}
