package com.campaignorganizer.backup;

import java.io.IOException;
import java.io.InputStream;

/**
 * Produces a database dump as a byte stream. Kept as a seam (ADR-0055) so
 * {@link BackupService}'s zip/media composition is testable without a real
 * Postgres or {@code pg_dump} binary.
 */
public interface PgDumpRunner {

    InputStream dump() throws IOException;
}
