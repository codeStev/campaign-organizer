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
import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushLimits;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushRecord;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort.MediaContentView;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@ExtendWith(MockitoExtension.class)
class FoundryPushServiceTest {

    private final UUID worldId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final UUID handoutId = UUID.randomUUID();
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
    private MediaContentQueryPort media;
    @Mock
    private TextEncryptor apiKeyEncryptor;
    @Mock
    private IdGenerator ids;

    private FoundryPushService service;

    @BeforeEach
    void setUp() {
        service = new FoundryPushService(connections, pushRecords, relay, articles, articleRenderer, handouts,
                media, apiKeyEncryptor, ids, clock);
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
