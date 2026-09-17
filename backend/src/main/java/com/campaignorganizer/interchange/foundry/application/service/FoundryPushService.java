package com.campaignorganizer.interchange.foundry.application.service;

import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryPushStatusUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryConnectionRepositoryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryPushRecordRepositoryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort.Credentials;
import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushLimits;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushRecord;
import com.campaignorganizer.interchange.foundry.domain.MediaEmbedRewriter;
import com.campaignorganizer.interchange.foundry.domain.StableFoundryId;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort.MediaContentView;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Push use cases for Foundry (ADR-0115) — pure composition over {@code worldbuilding}'s
 * published ports plus this context's own connection/push-tracking storage, mirroring
 * {@code CampaignCalendarService}'s style. {@link #pushDocument} is the entity-agnostic
 * core every entity-specific push (article now; handout/roll table/card deck in later
 * phases) calls through, so a future bulk "push everything" use case is additive.
 */
@Service
public class FoundryPushService implements PushArticleToFoundryUseCase, GetFoundryPushStatusUseCase {

    private final FoundryConnectionRepositoryPort connections;
    private final FoundryPushRecordRepositoryPort pushRecords;
    private final FoundryRelayPort relay;
    private final ArticleQueryPort articles;
    private final ArticleRenderPort articleRenderer;
    private final MediaContentQueryPort media;
    private final TextEncryptor apiKeyEncryptor;
    private final IdGenerator ids;
    private final Clock clock;

    public FoundryPushService(FoundryConnectionRepositoryPort connections,
                              FoundryPushRecordRepositoryPort pushRecords, FoundryRelayPort relay,
                              ArticleQueryPort articles, ArticleRenderPort articleRenderer,
                              MediaContentQueryPort media,
                              @Qualifier("foundryApiKeyEncryptor") TextEncryptor apiKeyEncryptor, IdGenerator ids,
                              Clock clock) {
        this.connections = connections;
        this.pushRecords = pushRecords;
        this.relay = relay;
        this.articles = articles;
        this.articleRenderer = articleRenderer;
        this.media = media;
        this.apiKeyEncryptor = apiKeyEncryptor;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public FoundryPushResult push(UUID worldId, UUID articleId) {
        ArticleView article = articles.findByIdInWorld(articleId, worldId)
                .orElseThrow(() -> new NotFoundException("Article not found"));
        Credentials credentials = credentialsFor(requireConnection(worldId));
        String markdownBody = articleRenderer.renderBodyAsMarkdown(worldId, article.body());
        return pushDocument(worldId, FoundryEntityType.ARTICLE, articleId, article.title(), markdownBody,
                credentials);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoundryPushStatusView> statusFor(UUID worldId, FoundryEntityType entityType, UUID entityId) {
        return pushRecords.findByEntity(worldId, entityType, entityId)
                .map(r -> new FoundryPushStatusView(r.getFoundryDocumentId(), r.getPushedAt()));
    }

    /** Folder-upsert, embedded-media upload, journal-upsert, push-record upsert — the part of a
     * push that's identical for every entity type. Callers obtain the title/Markdown body their
     * own way (an article renders wiki-links first; a handout has no rendering step at all). */
    private FoundryPushResult pushDocument(UUID worldId, FoundryEntityType type, UUID entityId, String title,
                                           String markdownBody, Credentials credentials) {
        String folderId = upsertFolderFor(worldId, type, credentials);

        List<String> warnings = new ArrayList<>();
        String rewrittenBody = uploadEmbeddedMedia(worldId, markdownBody, credentials, warnings);

        String documentId = StableFoundryId.from(documentKey(worldId, type, entityId));
        relay.upsertJournalEntry(credentials, documentId, title, rewrittenBody, folderId);

        Instant now = clock.instant();
        FoundryPushRecord record = pushRecords.findByEntity(worldId, type, entityId)
                .map(r -> {
                    r.recordPush(documentId, now);
                    return r;
                })
                .orElseGet(() -> FoundryPushRecord.create(ids.newId(), worldId, type, entityId, documentId, now));
        pushRecords.save(record);

        return new FoundryPushResult(documentId, now, warnings);
    }

    private String upsertFolderFor(UUID worldId, FoundryEntityType type, Credentials credentials) {
        String folderId = StableFoundryId.from("campaign-organizer:" + worldId + ":folder:" + folderKey(type));
        relay.upsertFolder(credentials, folderId, folderName(type));
        return folderId;
    }

    private String uploadEmbeddedMedia(UUID worldId, String markdownBody, Credentials credentials,
                                       List<String> warnings) {
        var mediaIds = MediaEmbedRewriter.mediaIdsIn(markdownBody);
        if (mediaIds.isEmpty()) {
            return markdownBody;
        }
        Map<UUID, String> resolvedPaths = new HashMap<>();
        for (UUID mediaId : mediaIds) {
            Optional<MediaContentView> content = media.loadInWorld(mediaId, worldId);
            if (content.isEmpty()) {
                // Referenced media no longer exists in this world; leave the (now broken)
                // reference as-is rather than failing the whole push over it.
                continue;
            }
            MediaContentView view = content.get();
            if (view.bytes().length > FoundryPushLimits.MAX_IMAGE_BYTES) {
                warnings.add("Image too large to push to Foundry, skipped: " + view.filename());
                continue;
            }
            String filename = StableFoundryId.from("campaign-organizer:" + worldId + ":media:" + mediaId)
                    + extensionFor(view.contentType());
            String path = relay.uploadFile(credentials, "campaign-organizer/" + worldId, filename,
                    view.contentType(), view.bytes());
            resolvedPaths.put(mediaId, path);
        }
        return MediaEmbedRewriter.rewrite(markdownBody, resolvedPaths::get);
    }

    private static String documentKey(UUID worldId, FoundryEntityType type, UUID entityId) {
        return "campaign-organizer:" + worldId + ":" + type.name().toLowerCase(Locale.ROOT) + ":" + entityId;
    }

    private static String folderKey(FoundryEntityType type) {
        return switch (type) {
            case ARTICLE -> "articles";
            case HANDOUT -> "handouts";
            case ROLL_TABLE -> "rolltables";
            case CARD_DECK -> "carddecks";
        };
    }

    private static String folderName(FoundryEntityType type) {
        return switch (type) {
            case ARTICLE -> "Articles";
            case HANDOUT -> "Handouts";
            case ROLL_TABLE -> "Roll Tables";
            case CARD_DECK -> "Card Decks";
        };
    }

    /** Extensions for {@code MediaAsset}'s own fixed, closed set of allowed content types —
     * never user-controlled input, so no escaping concern when used to build a filename. */
    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            default -> "";
        };
    }

    private FoundryConnection requireConnection(UUID worldId) {
        return connections.findByWorldId(worldId)
                .orElseThrow(() -> new NotFoundException("No Foundry connection configured for this world"));
    }

    private Credentials credentialsFor(FoundryConnection c) {
        return new Credentials(c.getRelayBaseUrl(), apiKeyEncryptor.decrypt(c.getApiKeyEncrypted()),
                c.getClientId());
    }
}
