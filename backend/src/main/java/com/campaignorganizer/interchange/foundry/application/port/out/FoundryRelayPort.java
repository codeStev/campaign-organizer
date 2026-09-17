package com.campaignorganizer.interchange.foundry.application.port.out;

import java.util.List;

/**
 * Outbound port to the self-hosted Foundry relay (ADR-0115) — a third-party
 * generic HTTP+JSON bridge in front of a live Foundry VTT session.
 */
public interface FoundryRelayPort {

    /** Which Foundry session a call targets, and the credentials to reach the relay with. */
    record Credentials(String relayBaseUrl, String apiKey, String clientId) {
    }

    /** {@code clientId}s of every Foundry session currently connected to the relay. */
    List<String> listConnectedClients(Credentials credentials);

    /** Idempotent upsert (via {@code keepId}/{@code override}) of a Folder document holding
     * JournalEntry (or, in later phases, RollTable/Cards) documents of one kind. */
    void upsertFolder(Credentials credentials, String folderId, String name);

    /** Idempotent upsert of a single-page Markdown JournalEntry document, placed in
     * {@code folderId}. */
    void upsertJournalEntry(Credentials credentials, String documentId, String name, String markdownBody,
                            String folderId);

    /** Uploads {@code bytes} into Foundry's own {@code Data/} storage at
     * {@code targetDir}/{@code filename} (overwriting any existing file at that exact path,
     * which is what makes re-uploading the same server-derived stable path idempotent).
     * Returns whatever path the relay reports back — used verbatim, never reconstructed. */
    String uploadFile(Credentials credentials, String targetDir, String filename, String contentType, byte[] bytes);
}
