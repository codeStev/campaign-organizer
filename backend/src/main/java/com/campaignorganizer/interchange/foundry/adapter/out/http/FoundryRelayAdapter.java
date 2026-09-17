package com.campaignorganizer.interchange.foundry.adapter.out.http;

import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort;
import com.campaignorganizer.interchange.foundry.domain.FoundryRelayException;
import com.campaignorganizer.interchange.foundry.domain.StableFoundryId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class FoundryRelayAdapter implements FoundryRelayPort {

    @Override
    public List<String> listConnectedClients(Credentials credentials) {
        return call(credentials, client -> client.listClients());
    }

    @Override
    public void upsertFolder(Credentials credentials, String folderId, String name) {
        call(credentials, client -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("_id", folderId);
            data.put("name", name);
            // Foundry Folders are typed by the kind of document they hold — every Folder
            // this feature creates today holds JournalEntry documents.
            data.put("type", "JournalEntry");
            client.create("Folder", data, null);
            return null;
        });
    }

    @Override
    public void upsertJournalEntry(Credentials credentials, String documentId, String name, String markdownBody,
                                   String htmlBody, String folderId) {
        call(credentials, client -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("_id", documentId);
            data.put("name", name);
            data.put("pages", List.of(buildPage(documentId, name, markdownBody, htmlBody)));
            client.create("JournalEntry", data, folderId);
            return null;
        });
    }

    /** Package-visible for {@code FoundryRelayAdapterTest} — pure map-building, no HTTP. */
    static Map<String, Object> buildPage(String documentId, String name, String markdownBody, String htmlBody) {
        Map<String, Object> page = new LinkedHashMap<>();
        // A JournalEntryPage is itself an embedded document keyed by its own _id, distinct
        // from the parent JournalEntry's _id. Foundry's embedded-collection update semantics
        // upsert an incoming "pages" array by that id; without one, every push sent a page
        // with no stable identity, so each re-push was treated as a brand-new page to ADD
        // rather than the existing one to replace — confirmed live: repeatedly pushing the
        // same article accumulated a duplicate page per push instead of updating one in
        // place. Deriving it from documentId (itself already a stable hash) keeps it
        // deterministic without needing the original semantic key here.
        page.put("_id", StableFoundryId.from(documentId + ":page"));
        page.put("name", name);
        page.put("type", "text");
        // format 2 = Markdown (Foundry's JournalEntryPage text format) — but "content" is
        // what Foundry's journal viewer actually renders; "markdown" only feeds Foundry's
        // own Markdown editing sheet when a page is opened for editing there. Sending
        // markdown alone (this feature's original design) left every pushed page
        // rendering empty — confirmed against a real relay+Foundry instance, ADR-0115.
        page.put("text", Map.of("format", 2, "markdown", markdownBody, "content", htmlBody));
        return page;
    }

    @Override
    public String uploadFile(Credentials credentials, String targetDir, String filename, String contentType,
                             byte[] bytes) {
        return call(credentials, client -> client.upload(targetDir, "data", filename, contentType, bytes));
    }

    @Override
    public void upsertRollTable(Credentials credentials, String documentId, String name, String formula,
                                List<TableResultData> results, String folderId) {
        call(credentials, client -> {
            List<Map<String, Object>> resultMaps = new ArrayList<>();
            for (TableResultData result : results) {
                Map<String, Object> resultMap = new LinkedHashMap<>();
                resultMap.put("_id", result.id());
                resultMap.put("range", List.of(result.rangeMin(), result.rangeMax()));
                resultMap.put("description", result.description());
                // Foundry's exact TableResult.type string constants for this version are not
                // yet confirmed against a live instance (ADR-0115) — "text"/"document" are a
                // documented best-effort guess, not a verified fact.
                resultMap.put("type", result.type());
                if (result.documentUuid() != null) {
                    resultMap.put("documentUuid", result.documentUuid());
                }
                resultMaps.add(resultMap);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("_id", documentId);
            data.put("name", name);
            data.put("formula", formula);
            data.put("results", resultMaps);
            client.create("RollTable", data, folderId);
            return null;
        });
    }

    @Override
    public void upsertCardDeck(Credentials credentials, String documentId, String name, List<CardData> cards,
                               String folderId) {
        call(credentials, client -> {
            List<Map<String, Object>> cardMaps = new ArrayList<>();
            for (CardData card : cards) {
                Map<String, Object> cardMap = new LinkedHashMap<>();
                cardMap.put("_id", card.id());
                cardMap.put("name", card.name());
                cardMap.put("description", card.description());
                cardMaps.add(cardMap);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("_id", documentId);
            data.put("name", name);
            // This feature only ever creates a plain deck (never a hand/pile) — Foundry's
            // Cards.type distinguishes the three.
            data.put("type", "deck");
            data.put("cards", cardMaps);
            client.create("Cards", data, folderId);
            return null;
        });
    }

    private <T> T call(Credentials credentials, Function<FoundryRelayClient, T> call) {
        try {
            FoundryRelayClient client = new FoundryRelayClient(credentials.relayBaseUrl(), credentials.apiKey(),
                    credentials.clientId());
            return call.apply(client);
        } catch (RestClientException e) {
            // Never interpolate the API key or a raw exception message here — some
            // HTTP client exceptions echo request details. Only the relay's own
            // base URL (never secret) and the failure's status/type are safe to surface.
            throw new FoundryRelayException(
                    "Could not reach the Foundry relay at " + credentials.relayBaseUrl() + " ("
                            + describe(e) + ")");
        }
    }

    private static String describe(RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            return "HTTP " + responseException.getStatusCode().value();
        }
        return e.getClass().getSimpleName();
    }
}
