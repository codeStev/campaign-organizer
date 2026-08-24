package com.campaignorganizer.interchange.export.application.port.in;

import java.util.Map;
import java.util.UUID;

/**
 * Imports one world bundle as brand-new data (ADR-0061): every entity gets a
 * fresh id, with every reference to another entity in the same bundle
 * rewritten consistently. Used identically for additive import and, after
 * the caller has cleared existing worlds, for full-overwrite import — the
 * two modes differ only in what happens *before* this is called.
 */
public interface ImportBackupUseCase {

    /**
     * @param worldBundleJson the world's export bundle (ExportService's shape), as raw JSON bytes
     * @param mediaByOldId    that world's media bytes, keyed by the id they had in the bundle
     */
    void importWorld(byte[] worldBundleJson, Map<UUID, byte[]> mediaByOldId);
}
