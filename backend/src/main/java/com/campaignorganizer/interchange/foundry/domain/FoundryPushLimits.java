package com.campaignorganizer.interchange.foundry.domain;

/**
 * Size limits specific to pushing content to Foundry (ADR-0115) — separate
 * from, and lower than, this app's own 50MB media upload limit. Base64
 * inflates a payload roughly a third before it crosses the relay's
 * HTTP+WebSocket hop into a live Foundry client, a meaningfully different
 * cost profile than this app's own disk storage; the relay itself documents
 * a 250MB ceiling on base64-encoded upload data. A few MB is already
 * generous for a practical article/handout image.
 */
public final class FoundryPushLimits {

    public static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;

    private FoundryPushLimits() {
    }
}
