package com.campaignorganizer.interchange.foundry.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.campaignorganizer.handouts.application.port.published.HandoutQueryPort;
import com.campaignorganizer.handouts.application.port.published.HandoutView;
import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase.FoundryPushResult;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryConnectionRepositoryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryPushRecordRepositoryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort.TableResultData;
import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushLimits;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushRecord;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort.MediaContentView;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableEntryView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@ExtendWith(MockitoExtension.class)
class FoundryPushServiceTest {

    private final UUID worldId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final UUID handoutId = UUID.randomUUID();
    private final UUID rollTableId = UUID.randomUUID();
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private FoundryConnectionRepositoryPort connections;
    @Mock
    private FoundryPushRecordRepositoryPort pushRecords;
    @Mock
    private FoundryRelayPort relay;
    @Mock
    private ArticleQueryPort articles;
    @Mock
    private ArticleRenderPort articleRenderer;
    @Mock
    private HandoutQueryPort handouts;
    @Mock
    private RollTableQueryPort rollTables;
    @Mock
    private MediaContentQueryPort media;
    @Mock
    private TextEncryptor apiKeyEncryptor;
    @Mock
    private IdGenerator ids;

    private FoundryPushService service;

    @BeforeEach
    void setUp() {
        service = new FoundryPushService(connections, pushRecords, relay, articles, articleRenderer, handouts,
                rollTables, media, apiKeyEncryptor, ids, clock);
    }

    @Test
    void push_articleNotFound_throwsNotFound() {
        when(articles.findByIdInWorld(articleId, worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.push(worldId, articleId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Article");
    }

    @Test
    void push_noConnectionConfigured_throwsNotFound() {
        when(articles.findByIdInWorld(articleId, worldId)).thenReturn(Optional.of(article("body")));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.push(worldId, articleId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Foundry connection");
    }

    @Test
    void push_happyPath_rendersMarkdownUpsertsFolderAndJournalAndRecordsThePush() {
        when(articles.findByIdInWorld(articleId, worldId)).thenReturn(Optional.of(article("Hello [[world]]")));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(articleRenderer.renderBodyAsMarkdown(worldId, "Hello [[world]]")).thenReturn("Hello **World**");
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ARTICLE, articleId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoundryPushResult result = service.push(worldId, articleId);

        assertThat(result.foundryDocumentId()).matches("^[A-Za-z0-9]{16}$");
        assertThat(result.pushedAt()).isEqualTo(clock.instant());
        assertThat(result.warnings()).isEmpty();

        verify(relay).upsertFolder(eq(expectedCredentials()), anyString(), eq("Articles"));
        verify(relay).upsertJournalEntry(eq(expectedCredentials()), eq(result.foundryDocumentId()),
                eq("An Article"), eq("Hello **World**"), anyString());
        verify(pushRecords).save(any(FoundryPushRecord.class));
        // The article render port is called exactly once, for the article body — never
        // re-invoked mid-push (e.g. by anything embedded-media related).
        verify(articleRenderer, times(1)).renderBodyAsMarkdown(any(), any());
    }

    @Test
    void push_repeatedPush_resolvesToTheSameStableDocumentId() {
        when(articles.findByIdInWorld(articleId, worldId)).thenReturn(Optional.of(article("plain body")));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(articleRenderer.renderBodyAsMarkdown(worldId, "plain body")).thenReturn("plain body");
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ARTICLE, articleId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoundryPushResult first = service.push(worldId, articleId);
        FoundryPushResult second = service.push(worldId, articleId);

        assertThat(second.foundryDocumentId()).isEqualTo(first.foundryDocumentId());
    }

    @Test
    void push_updatesExistingPushRecordInstead_ofCreatingASecondOne() {
        when(articles.findByIdInWorld(articleId, worldId)).thenReturn(Optional.of(article("plain body")));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(articleRenderer.renderBodyAsMarkdown(worldId, "plain body")).thenReturn("plain body");
        FoundryPushRecord existing = FoundryPushRecord.reconstitute(UUID.randomUUID(), worldId,
                FoundryEntityType.ARTICLE, articleId, "existingdocid1234", Instant.EPOCH);
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ARTICLE, articleId))
                .thenReturn(Optional.of(existing));
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.push(worldId, articleId);

        verify(ids, never()).newId();
        verify(pushRecords).save(existing);
    }

    @Test
    void push_embeddedImageWithinLimit_isUploadedAndBodyRewritten() {
        UUID mediaId = UUID.randomUUID();
        String body = "![img](/api/media/" + mediaId + "/content)";
        when(articles.findByIdInWorld(articleId, worldId)).thenReturn(Optional.of(article(body)));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(articleRenderer.renderBodyAsMarkdown(worldId, body)).thenReturn(body);
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ARTICLE, articleId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(media.loadInWorld(mediaId, worldId))
                .thenReturn(Optional.of(new MediaContentView("pic.png", "image/png", new byte[]{1, 2, 3})));
        when(relay.uploadFile(any(), anyString(), anyString(), eq("image/png"), any()))
                .thenReturn("campaign-organizer/" + worldId + "/uploaded.png");

        FoundryPushResult result = service.push(worldId, articleId);

        assertThat(result.warnings()).isEmpty();
        verify(relay).uploadFile(eq(expectedCredentials()), eq("campaign-organizer/" + worldId), anyString(),
                eq("image/png"), eq(new byte[]{1, 2, 3}));
        verify(relay).upsertJournalEntry(any(), anyString(), anyString(),
                eq("![img](campaign-organizer/" + worldId + "/uploaded.png)"), anyString());
    }

    @Test
    void push_oversizedEmbeddedImage_isSkippedWithWarningNotUploaded() {
        UUID mediaId = UUID.randomUUID();
        String body = "![img](/api/media/" + mediaId + "/content)";
        when(articles.findByIdInWorld(articleId, worldId)).thenReturn(Optional.of(article(body)));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(articleRenderer.renderBodyAsMarkdown(worldId, body)).thenReturn(body);
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ARTICLE, articleId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));
        byte[] tooBig = new byte[(int) FoundryPushLimits.MAX_IMAGE_BYTES + 1];
        when(media.loadInWorld(mediaId, worldId))
                .thenReturn(Optional.of(new MediaContentView("big.png", "image/png", tooBig)));

        FoundryPushResult result = service.push(worldId, articleId);

        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().get(0)).contains("big.png");
        verify(relay, never()).uploadFile(any(), any(), any(), any(), any());
        // Body is left with the original (now-unresolved) reference, not blanked out.
        verify(relay).upsertJournalEntry(any(), anyString(), anyString(), eq(body), anyString());
    }

    @Test
    void pushHandout_handoutNotFound_throwsNotFound() {
        when(handouts.findByIdInWorld(handoutId, worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pushHandout(worldId, handoutId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Handout");
    }

    @Test
    void pushHandout_noConnectionConfigured_throwsNotFound() {
        when(handouts.findByIdInWorld(handoutId, worldId)).thenReturn(Optional.of(handout("body")));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pushHandout(worldId, handoutId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Foundry connection");
    }

    @Test
    void pushHandout_happyPath_bodyPushedVerbatimWithNoRendering() {
        when(handouts.findByIdInWorld(handoutId, worldId))
                .thenReturn(Optional.of(handout("Raw [[not-a-link]] body")));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(pushRecords.findByEntity(worldId, FoundryEntityType.HANDOUT, handoutId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoundryPushResult result = service.pushHandout(worldId, handoutId);

        assertThat(result.foundryDocumentId()).matches("^[A-Za-z0-9]{16}$");
        assertThat(result.warnings()).isEmpty();

        verify(relay).upsertFolder(eq(expectedCredentials()), anyString(), eq("Handouts"));
        // The whole point of this phase: the body reaches the relay completely unrendered —
        // a handout has no wiki-link resolution step, unlike an article.
        verify(relay).upsertJournalEntry(eq(expectedCredentials()), eq(result.foundryDocumentId()),
                eq("A Handout"), eq("Raw [[not-a-link]] body"), anyString());
        verify(pushRecords).save(any(FoundryPushRecord.class));
        verifyNoInteractions(articleRenderer);
    }

    @Test
    void pushHandout_articleAndHandoutFoldersAreKeptSeparate() {
        when(handouts.findByIdInWorld(handoutId, worldId)).thenReturn(Optional.of(handout("body")));
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(pushRecords.findByEntity(worldId, FoundryEntityType.HANDOUT, handoutId)).thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.pushHandout(worldId, handoutId);

        verify(relay, never()).upsertFolder(any(), anyString(), eq("Articles"));
        verify(relay).upsertFolder(any(), anyString(), eq("Handouts"));
    }

    @Test
    void pushRollTable_rollTableNotFound_throwsNotFound() {
        when(rollTables.existsInWorld(rollTableId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.pushRollTable(worldId, rollTableId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Roll table");
    }

    @Test
    void pushRollTable_noConnectionConfigured_throwsNotFound() {
        when(rollTables.existsInWorld(rollTableId, worldId)).thenReturn(true);
        when(connections.findByWorldId(worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.pushRollTable(worldId, rollTableId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Foundry connection");
    }

    @Test
    void pushRollTable_happyPath_upsertsFolderAndFormulaAndPlainTextResults() {
        RollTableEntryView entry1 = new RollTableEntryView(UUID.randomUUID(), 1, 3, "Goblin", List.of(), List.of());
        RollTableEntryView entry2 = new RollTableEntryView(UUID.randomUUID(), 4, 6, "Orc", List.of(), List.of());
        when(rollTables.existsInWorld(rollTableId, worldId)).thenReturn(true);
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(rollTables.findByIdInWorld(rollTableId, worldId))
                .thenReturn(Optional.of(rollTable(List.of(entry1, entry2))));
        when(articleRenderer.renderBody(worldId, "Goblin")).thenReturn("<p>Goblin</p>");
        when(articleRenderer.renderBody(worldId, "Orc")).thenReturn("<p>Orc</p>");
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ROLL_TABLE, rollTableId))
                .thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoundryPushResult result = service.pushRollTable(worldId, rollTableId);

        assertThat(result.foundryDocumentId()).matches("^[A-Za-z0-9]{16}$");
        assertThat(result.warnings()).isEmpty();
        verify(relay).upsertFolder(eq(expectedCredentials()), anyString(), eq("Roll Tables"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TableResultData>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(relay).upsertRollTable(eq(expectedCredentials()), eq(result.foundryDocumentId()), eq("A Table"),
                eq("1d6"), resultsCaptor.capture(), anyString());
        List<TableResultData> results = resultsCaptor.getValue();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).rangeMin()).isEqualTo(1);
        assertThat(results.get(0).rangeMax()).isEqualTo(3);
        assertThat(results.get(0).description()).isEqualTo("<p>Goblin</p>");
        assertThat(results.get(0).type()).isEqualTo("text");
        assertThat(results.get(0).documentUuid()).isNull();
        assertThat(results.get(1).description()).isEqualTo("<p>Orc</p>");
        verify(pushRecords).save(any(FoundryPushRecord.class));
    }

    @Test
    void pushRollTable_catchAllEntry_getsTheTablesFullRangeNotZeroZero() {
        // A null/null entry is this app's own catch-all row ("covers every result no explicit
        // entry claims" — RollTable.validateEntries). Regression test: an earlier version of
        // this mapping defaulted null bounds to 0, which is unreachable on any real dice roll
        // (minimum result >= 1) and would silently drop the fallback row once pushed to Foundry.
        RollTableEntryView explicit = new RollTableEntryView(UUID.randomUUID(), 1, 3, "Goblin", List.of(), List.of());
        RollTableEntryView catchAll = new RollTableEntryView(UUID.randomUUID(), null, null, "Nothing", List.of(),
                List.of());
        when(rollTables.existsInWorld(rollTableId, worldId)).thenReturn(true);
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(rollTables.findByIdInWorld(rollTableId, worldId))
                .thenReturn(Optional.of(rollTable(List.of(explicit, catchAll))));
        when(articleRenderer.renderBody(eq(worldId), any())).thenAnswer(inv -> "<p>" + inv.getArgument(1) + "</p>");
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ROLL_TABLE, rollTableId))
                .thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.pushRollTable(worldId, rollTableId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TableResultData>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(relay).upsertRollTable(any(), any(), any(), any(), resultsCaptor.capture(), any());
        TableResultData catchAllResult = resultsCaptor.getValue().get(1);
        // rollTable(...) builds tables with minResult=1, maxResult=6 (see the helper below).
        assertThat(catchAllResult.rangeMin()).isEqualTo(1);
        assertThat(catchAllResult.rangeMax()).isEqualTo(6);
    }

    @Test
    void pushRollTable_embeddedImageInEntryBody_isUploadedAndDescriptionRewritten() {
        UUID mediaId = UUID.randomUUID();
        String rendered = "<p><img src=\"/api/media/" + mediaId + "/content\"></p>";
        RollTableEntryView entry = new RollTableEntryView(UUID.randomUUID(), 1, 6, "raw", List.of(), List.of());
        when(rollTables.existsInWorld(rollTableId, worldId)).thenReturn(true);
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(rollTables.findByIdInWorld(rollTableId, worldId)).thenReturn(Optional.of(rollTable(List.of(entry))));
        when(articleRenderer.renderBody(worldId, "raw")).thenReturn(rendered);
        when(pushRecords.findByEntity(worldId, FoundryEntityType.ROLL_TABLE, rollTableId))
                .thenReturn(Optional.empty());
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(media.loadInWorld(mediaId, worldId))
                .thenReturn(Optional.of(new MediaContentView("pic.png", "image/png", new byte[]{1, 2, 3})));
        when(relay.uploadFile(any(), anyString(), anyString(), eq("image/png"), any()))
                .thenReturn("campaign-organizer/" + worldId + "/uploaded.png");

        service.pushRollTable(worldId, rollTableId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TableResultData>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(relay).upsertRollTable(any(), anyString(), anyString(), anyString(), resultsCaptor.capture(),
                anyString());
        assertThat(resultsCaptor.getValue().get(0).description())
                .isEqualTo("<p><img src=\"campaign-organizer/" + worldId + "/uploaded.png\"></p>");
    }

    @Test
    void pushRollTable_nestedTableChain_cycleGuardResolvesBothWithoutInfiniteRecursion() {
        UUID tableAId = rollTableId;
        UUID tableBId = UUID.randomUUID();
        RollTableEntryView entryA = new RollTableEntryView(UUID.randomUUID(), 1, 6, "goes to B",
                List.of(tableBId), List.of());
        RollTableEntryView entryB = new RollTableEntryView(UUID.randomUUID(), 1, 6, "back to A",
                List.of(tableAId), List.of());
        when(rollTables.existsInWorld(tableAId, worldId)).thenReturn(true);
        when(connections.findByWorldId(worldId)).thenReturn(Optional.of(connection()));
        when(apiKeyEncryptor.decrypt("enc-key")).thenReturn("plain-key");
        when(rollTables.findByIdInWorld(tableAId, worldId))
                .thenReturn(Optional.of(new RollTableView(tableAId, worldId, null, "Table A", null, "1d6", 1, 6,
                        List.of(entryA), Instant.EPOCH, Instant.EPOCH)));
        when(rollTables.findByIdInWorld(tableBId, worldId))
                .thenReturn(Optional.of(new RollTableView(tableBId, worldId, null, "Table B", null, "1d6", 1, 6,
                        List.of(entryB), Instant.EPOCH, Instant.EPOCH)));
        when(articleRenderer.renderBody(any(), any())).thenAnswer(inv -> inv.getArgument(1));
        when(pushRecords.findByEntity(any(), any(), any())).thenReturn(Optional.empty());
        when(ids.newId()).thenAnswer(inv -> UUID.randomUUID());
        when(pushRecords.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoundryPushResult result = service.pushRollTable(worldId, tableAId);

        // Both tables get pushed exactly once each — the cycle terminates rather than
        // recursing forever, and both results reference each other's deterministic id.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TableResultData>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(relay, times(2)).upsertRollTable(any(), anyString(), anyString(), anyString(),
                resultsCaptor.capture(), anyString());
        List<List<TableResultData>> allResults = resultsCaptor.getAllValues();
        String aDocId = result.foundryDocumentId();
        assertThat(allResults.get(0).get(0).type()).isEqualTo("document");
        assertThat(allResults.get(0).get(0).documentUuid()).isEqualTo("RollTable." + aDocId);
        // Table B's result must reference table A's own stable id, resolved via the
        // cycle guard without a second recursive push of A.
        String bReferencedId = allResults.get(1).get(0).documentUuid();
        assertThat(bReferencedId).startsWith("RollTable.");
        assertThat(aDocId).isNotBlank();
    }

    private RollTableView rollTable(List<RollTableEntryView> entries) {
        return new RollTableView(rollTableId, worldId, null, "A Table", null, "1d6", 1, 6, entries, Instant.EPOCH,
                Instant.EPOCH);
    }

    private HandoutView handout(String body) {
        return new HandoutView(handoutId, worldId, null, "A Handout", "PARCHMENT", body, null, null, false,
                Instant.EPOCH, Instant.EPOCH);
    }

    private FoundryRelayPort.Credentials expectedCredentials() {
        return new FoundryRelayPort.Credentials("https://relay.example.com", "plain-key", "my-client");
    }

    private ArticleView article(String body) {
        return new ArticleView(articleId, worldId, null, null, "An Article", "an-article", "default", body,
                Instant.EPOCH, Instant.EPOCH);
    }

    private FoundryConnection connection() {
        return FoundryConnection.reconstitute(worldId, "https://relay.example.com", "my-client", "enc-key",
                Instant.EPOCH, Instant.EPOCH);
    }
}
