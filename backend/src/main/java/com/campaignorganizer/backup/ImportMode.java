package com.campaignorganizer.backup;

/** How a backup import relates to whatever data already exists (ADR-0061). */
public enum ImportMode {
    /** The backup's worlds are added as new data; nothing existing is touched. */
    ADDITIVE,
    /** Every existing world is deleted first, then the backup's worlds are added. */
    OVERWRITE
}
