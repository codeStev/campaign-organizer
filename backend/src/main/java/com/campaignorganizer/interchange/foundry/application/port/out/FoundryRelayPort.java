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

    /** One {@code TableResult} row (ADR-0115) — field names/types confirmed against Foundry's
     * own official document-schema docs (foundryvtt.com/api), a different reference from the
     * relay's own transport docs: {@code range} is a plain {@code [min, max]} array, the body
     * content field is {@code description} (HTML, not Markdown — {@code TableResult} has no
     * Markdown mode unlike {@code JournalEntryPage}), and a chained/nested reference to another
     * document uses a single {@code documentUuid} string (e.g. {@code "RollTable.<id>"}), not
     * separate collection/id fields. {@code type}/{@code documentUuid} are {@code null} for a
     * plain-text result. <b>Not yet verified against a live Foundry instance:</b> the exact
     * string values Foundry uses for {@code type} (plain-text vs document-reference) — this
     * class uses {@code "text"}/{@code "document"} as a documented best-effort guess (see
     * {@code FoundryRelayAdapter}), not a confirmed fact. */
    record TableResultData(String id, int rangeMin, int rangeMax, String description, String type,
                           String documentUuid) {
    }

    /** One embedded {@code Card} (ADR-0115) — field names/types confirmed against Foundry's own
     * official document-schema docs (foundryvtt.com/api): {@code name} and {@code description}
     * (HTML, not Markdown — same as {@code TableResult}, {@code Card} has no Markdown mode
     * either). <b>Not yet verified against a live Foundry instance:</b> unlike {@code
     * TableResult}, the fetched class docs for {@code Card} did not confirm any
     * document-reference field analogous to {@code documentUuid} — this feature therefore
     * never attempts real Foundry document linking for a card's chained table/deck references;
     * see {@code FoundryPushService} for the plain-text-note fallback this uses instead. */
    record CardData(String id, String name, String description) {
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

    /** Idempotent upsert of a native {@code RollTable} document, placed in {@code folderId}. */
    void upsertRollTable(Credentials credentials, String documentId, String name, String formula,
                         List<TableResultData> results, String folderId);

    /** Idempotent upsert of a native {@code Cards} document (always {@code type: "deck"} for
     * this feature), placed in {@code folderId}. */
    void upsertCardDeck(Credentials credentials, String documentId, String name, List<CardData> cards,
                        String folderId);
}
