package com.campaignorganizer.interchange.foundry.application.service;

import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryPushStatusUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCampaignToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCategoryToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushHandoutToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushCardDeckToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushRollTableToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushSessionToFoundryUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.PushWorldWikiToFoundryUseCase;
import com.campaignorganizer.interchange.packet.application.port.in.BuildSessionPacketUseCase;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.SessionPacketResponse;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryConnectionRepositoryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryPushRecordRepositoryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort.Credentials;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort.CardData;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort.TableResultData;
import com.campaignorganizer.interchange.foundry.domain.FoundryCategoryPushMode;
import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import com.campaignorganizer.interchange.foundry.domain.FoundryDocumentLinkRewriter;
import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushLimits;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushRecord;
import com.campaignorganizer.interchange.foundry.domain.MediaEmbedRewriter;
import com.campaignorganizer.interchange.foundry.domain.StableFoundryId;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort.JournalPageData;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort;
import com.campaignorganizer.media.application.port.published.MediaContentQueryPort.MediaContentView;
import com.campaignorganizer.handouts.application.port.published.HandoutQueryPort;
import com.campaignorganizer.handouts.application.port.published.HandoutView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.carddeck.port.published.DeckCardView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableEntryView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Push use cases for Foundry (ADR-0115) — pure composition over {@code worldbuilding}'s
 * published ports plus this context's own connection/push-tracking storage, mirroring
 * {@code CampaignCalendarService}'s style. {@link #pushDocument} is the entity-agnostic
 * core every entity-specific push (article and handout now; roll table/card deck in later
 * phases) calls through, so a future bulk "push everything" use case is additive.
 */
@Service
public class FoundryPushService implements PushArticleToFoundryUseCase, PushHandoutToFoundryUseCase,
        PushRollTableToFoundryUseCase, PushCardDeckToFoundryUseCase, PushSessionToFoundryUseCase,
        PushCategoryToFoundryUseCase, PushWorldWikiToFoundryUseCase, PushCampaignToFoundryUseCase,
        GetFoundryPushStatusUseCase {

    /** Best-effort {@code TableResult.type} values (ADR-0115) — Foundry's official class docs
     * confirm the field exists but not its concrete strings for the version in use; these are
     * this feature's documented guess pending live verification, not a confirmed fact. */
    private static final String RESULT_TYPE_TEXT = "text";
    private static final String RESULT_TYPE_DOCUMENT = "document";

    private final FoundryConnectionRepositoryPort connections;
    private final FoundryPushRecordRepositoryPort pushRecords;
    private final FoundryRelayPort relay;
    private final ArticleQueryPort articles;
    private final ArticleRenderPort articleRenderer;
    private final CategoryQueryPort categories;
    private final HandoutQueryPort handouts;
    private final RollTableQueryPort rollTables;
    private final CardDeckQueryPort cardDecks;
    private final MediaContentQueryPort media;
    private final TextEncryptor apiKeyEncryptor;
    private final IdGenerator ids;
    private final Clock clock;
    private final BuildSessionPacketUseCase sessionPacket;
    private final SessionQueryPort sessions;
    private final ArcBeatQueryPort arcBeats;
    // Package-private, not final: Spring always supplies the real proxy via the constructor
    // below in production. Tests in this package construct a plain instance (no Spring
    // context, no proxy) and assign this directly afterward, since the constructor can't
    // reference the object currently being constructed.
    FoundryPushService self;

    public FoundryPushService(FoundryConnectionRepositoryPort connections,
                              FoundryPushRecordRepositoryPort pushRecords, FoundryRelayPort relay,
                              ArticleQueryPort articles, ArticleRenderPort articleRenderer,
                              CategoryQueryPort categories, HandoutQueryPort handouts, RollTableQueryPort rollTables,
                              CardDeckQueryPort cardDecks, MediaContentQueryPort media,
                              @Qualifier("foundryApiKeyEncryptor") TextEncryptor apiKeyEncryptor, IdGenerator ids,
                              Clock clock, BuildSessionPacketUseCase sessionPacket, SessionQueryPort sessions,
                              ArcBeatQueryPort arcBeats, @Lazy FoundryPushService self) {
        this.connections = connections;
        this.pushRecords = pushRecords;
        this.relay = relay;
        this.articles = articles;
        this.articleRenderer = articleRenderer;
        this.categories = categories;
        this.handouts = handouts;
        this.rollTables = rollTables;
        this.cardDecks = cardDecks;
        this.media = media;
        this.apiKeyEncryptor = apiKeyEncryptor;
        this.ids = ids;
        this.clock = clock;
        this.sessionPacket = sessionPacket;
        this.sessions = sessions;
        this.arcBeats = arcBeats;
        // Self-injected proxy (ADR-0115) so pushSession's delegated calls below go through
        // Spring's real @Transactional interception instead of bypassing it via plain
        // self-invocation (calling `this.push(...)` directly would silently skip that
        // method's own @Transactional, since AOP proxying only applies to calls arriving
        // through the proxy, never to a method calling a sibling method on itself). This
        // keeps each entity's push in its own short transaction rather than requiring
        // pushSession to hold one long transaction open across many outbound relay HTTP
        // calls for every entity in the session.
        this.self = self;
    }

    @Override
    @Transactional
    public FoundryPushResult push(UUID worldId, UUID articleId) {
        ArticleView article = articles.findByIdInWorld(articleId, worldId)
                .orElseThrow(() -> new NotFoundException("Article not found"));
        Credentials credentials = credentialsFor(requireConnection(worldId));
        String markdownBody = articleRenderer.renderBodyAsMarkdown(worldId, article.body());
        String folderId = upsertFolderFor(worldId, FoundryEntityType.ARTICLE, credentials);
        return pushDocument(worldId, FoundryEntityType.ARTICLE, articleId, article.title(), markdownBody, folderId,
                credentials);
    }

    @Override
    @Transactional
    public FoundryPushResult pushHandout(UUID worldId, UUID handoutId) {
        HandoutView handout = handouts.findByIdInWorld(handoutId, worldId)
                .orElseThrow(() -> new NotFoundException("Handout not found"));
        Credentials credentials = credentialsFor(requireConnection(worldId));
        String folderId = upsertFolderFor(worldId, FoundryEntityType.HANDOUT, credentials);
        return pushDocument(worldId, FoundryEntityType.HANDOUT, handoutId, handout.title(), handout.body(), folderId,
                credentials);
    }

    @Override
    @Transactional
    public FoundryPushResult pushRollTable(UUID worldId, UUID rollTableId) {
        if (!rollTables.existsInWorld(rollTableId, worldId)) {
            throw new NotFoundException("Roll table not found");
        }
        Credentials credentials = credentialsFor(requireConnection(worldId));
        List<String> warnings = new ArrayList<>();
        String documentId = ensureRollTablePushed(worldId, rollTableId, credentials, new LinkedHashSet<>(),
                warnings);
        return new FoundryPushResult(documentId, clock.instant(), warnings);
    }

    /** Pushes one roll table's {@code RollTable} document, recursing into any
     * {@code nestedTableIds} an entry chains to so those tables exist in Foundry (and have a
     * stable id to link to) before this table references them. {@code inProgress} guards
     * reference cycles the same way this app's own domain already treats them at resolution
     * time — "cut, not rejected": if a nested id is already being pushed higher up the same
     * call stack, this returns its deterministic stable id directly instead of recursing again,
     * so table A referencing B referencing back to A terminates rather than looping forever. */
    private String ensureRollTablePushed(UUID worldId, UUID rollTableId, Credentials credentials,
                                         Set<UUID> inProgress, List<String> warnings) {
        String documentId = StableFoundryId.from(documentKey(worldId, FoundryEntityType.ROLL_TABLE, rollTableId));
        if (!inProgress.add(rollTableId)) {
            return documentId;
        }
        RollTableView table = rollTables.findByIdInWorld(rollTableId, worldId)
                .orElseThrow(() -> new NotFoundException("Roll table not found"));

        String folderId = upsertFolderFor(worldId, FoundryEntityType.ROLL_TABLE, credentials);
        List<TableResultData> results = new ArrayList<>();
        for (RollTableEntryView entry : table.entries()) {
            results.add(toTableResult(worldId, entry, table.minResult(), table.maxResult(), credentials,
                    inProgress, warnings));
        }
        relay.upsertRollTable(credentials, documentId, table.title(), table.diceExpression(), results, folderId);
        recordPush(worldId, FoundryEntityType.ROLL_TABLE, rollTableId, documentId);
        return documentId;
    }

    private TableResultData toTableResult(UUID worldId, RollTableEntryView entry, int tableMinResult,
                                          int tableMaxResult, Credentials credentials, Set<UUID> inProgress,
                                          List<String> warnings) {
        String resultId = StableFoundryId.from(
                "campaign-organizer:" + worldId + ":rolltable-entry:" + entry.id());
        String html = articleRenderer.renderBody(worldId, entry.body() == null ? "" : entry.body());
        String description = uploadEmbeddedMedia(worldId, html == null ? "" : html, credentials, warnings);
        // A null/null entry is this app's own catch-all row — "covers every result no explicit
        // entry claims" (RollTable.validateEntries). Foundry's TableResult has no equivalent
        // "unclaimed range" concept, so the closest faithful single-row translation is the
        // table's own full range; mapping it to [0,0] instead (as an earlier version of this
        // method did) would make the row unreachable for any real dice expression (minimum
        // roll >= 1), silently dropping the fallback the moment it's pushed to Foundry.
        int min = entry.minResult() == null ? tableMinResult : entry.minResult();
        int max = entry.maxResult() == null ? tableMaxResult : entry.maxResult();

        // At most one document reference per result — Foundry's TableResult has a single
        // documentUuid field, not a list. A nested table beyond the first, and every nested
        // deck (Card Deck push doesn't exist yet), fall back to a plain-text note rather than
        // being silently dropped or crashing the push.
        List<UUID> nestedTables = entry.nestedTableIds();
        if (!nestedTables.isEmpty()) {
            UUID primary = nestedTables.get(0);
            String nestedDocumentId = ensureRollTablePushed(worldId, primary, credentials, inProgress, warnings);
            if (nestedTables.size() > 1 || !entry.nestedDeckIds().isEmpty()) {
                warnings.add("Roll table entry chains to more than one table/deck; only the first "
                        + "nested table is linked as a Foundry document reference, the rest were skipped.");
            }
            return new TableResultData(resultId, min, max, description, RESULT_TYPE_DOCUMENT,
                    "RollTable." + nestedDocumentId);
        }
        if (!entry.nestedDeckIds().isEmpty()) {
            warnings.add("Roll table entry chains to a card deck, which isn't pushed to Foundry yet "
                    + "(Card Deck push is a later phase) — left as plain text.");
        }
        return new TableResultData(resultId, min, max, description, RESULT_TYPE_TEXT, null);
    }

    @Override
    @Transactional
    public FoundryPushResult pushCardDeck(UUID worldId, UUID cardDeckId) {
        if (!cardDecks.existsInWorld(cardDeckId, worldId)) {
            throw new NotFoundException("Card deck not found");
        }
        Credentials credentials = credentialsFor(requireConnection(worldId));
        List<String> warnings = new ArrayList<>();
        String documentId = ensureCardDeckPushed(worldId, cardDeckId, credentials, new LinkedHashSet<>(), warnings);
        return new FoundryPushResult(documentId, clock.instant(), warnings);
    }

    /** Pushes one card deck's {@code Cards} document. Unlike a roll table result, a {@code Card}
     * has no confirmed Foundry field for a real document reference (see {@code CardData}'s
     * javadoc), so a card's {@code nestedTableIds}/{@code nestedDeckIds} never trigger recursive
     * pushes here — {@code inProgress} is still threaded through for symmetry with {@code
     * ensureRollTablePushed} and in case a future confirmed reference field needs it, but is not
     * currently consulted for cycle detection since nothing recurses. */
    private String ensureCardDeckPushed(UUID worldId, UUID cardDeckId, Credentials credentials,
                                        Set<UUID> inProgress, List<String> warnings) {
        String documentId = StableFoundryId.from(documentKey(worldId, FoundryEntityType.CARD_DECK, cardDeckId));
        if (!inProgress.add(cardDeckId)) {
            return documentId;
        }
        CardDeckView deck = cardDecks.findByIdInWorld(cardDeckId, worldId)
                .orElseThrow(() -> new NotFoundException("Card deck not found"));

        String folderId = upsertFolderFor(worldId, FoundryEntityType.CARD_DECK, credentials);
        List<CardData> cards = new ArrayList<>();
        for (DeckCardView card : deck.cards()) {
            cards.add(toCardData(worldId, card, credentials, warnings));
        }
        relay.upsertCardDeck(credentials, documentId, deck.title(), cards, folderId);
        recordPush(worldId, FoundryEntityType.CARD_DECK, cardDeckId, documentId);
        return documentId;
    }

    private CardData toCardData(UUID worldId, DeckCardView card, Credentials credentials, List<String> warnings) {
        String cardId = StableFoundryId.from("campaign-organizer:" + worldId + ":carddeck-card:" + card.id());
        String html = articleRenderer.renderBody(worldId, card.body() == null ? "" : card.body());
        String description = uploadEmbeddedMedia(worldId, html == null ? "" : html, credentials, warnings);
        // A blank/null title is this app's own "no face title" state (DeckCard's javadoc calls
        // it an "optional face title"), not a validation gap — NextTablesView.tsx's own card
        // list falls back to the plain string "Card" for display, so this mirrors that existing
        // convention rather than inventing a new one.
        String name = card.title() == null || card.title().isBlank() ? "Card" : card.title();

        // Unlike a roll table entry, a card's chained table/deck references (FR-41) can't become
        // a real Foundry document reference — Card has no confirmed field for one (see CardData's
        // javadoc) — so they're surfaced as a plain-text note plus a push warning instead of
        // being silently dropped.
        if (!card.nestedTableIds().isEmpty() || !card.nestedDeckIds().isEmpty()) {
            description += "<p><em>Chains to " + (card.nestedTableIds().size() + card.nestedDeckIds().size())
                    + " other table(s)/deck(s) in Campaign Organizer — not linked here.</em></p>";
            warnings.add("Card \"" + name + "\" chains to another table/deck; Foundry's Card schema has no "
                    + "confirmed document-reference field, so this was left as a plain-text note instead.");
        }
        return new CardData(cardId, name, description);
    }

    /** Pushes everything the existing "print session packet" feature (ADR-0036) already
     * discovers for one session — referenced articles, session handouts, and any roll
     * tables/card decks the session's beats chain to — by calling this class's own
     * per-entity push methods through {@link #self} (see the constructor's comment on
     * why: plain self-invocation would silently skip each one's own {@code @Transactional}).
     * Deliberately no {@code @Transactional} here: this method does no persistence of its
     * own, only delegates, and each delegated call already opens its own short
     * transaction — wrapping the whole loop in one outer transaction would hold a single
     * DB transaction open across every relay HTTP call for every entity in the session. */
    @Override
    public FoundrySessionPushResult pushSession(UUID worldId, UUID campaignId, UUID sessionId) {
        SessionView session = sessions.findByIdInCampaign(sessionId, campaignId)
                .orElseThrow(() -> new NotFoundException("Session not found in campaign"));
        SessionPacketResponse packet = sessionPacket.packet(worldId, campaignId, sessionId);
        List<String> warnings = new ArrayList<>();

        int articlesPushed = 0;
        for (var article : packet.articles()) {
            warnings.addAll(self.push(worldId, article.id()).warnings());
            articlesPushed++;
        }
        int handoutsPushed = 0;
        for (var handout : packet.handouts()) {
            warnings.addAll(self.pushHandout(worldId, handout.id()).warnings());
            handoutsPushed++;
        }
        int rollTablesPushed = 0;
        for (var rollTable : packet.rollTables()) {
            warnings.addAll(self.pushRollTable(worldId, rollTable.id()).warnings());
            rollTablesPushed++;
        }
        int cardDecksPushed = 0;
        for (var cardDeck : packet.cardDecks()) {
            warnings.addAll(self.pushCardDeck(worldId, cardDeck.id()).warnings());
            cardDecksPushed++;
        }

        List<ArcBeatView> beats = new ArrayList<>(arcBeats.findBySession(sessionId));
        beats.sort(Comparator.comparingInt(ArcBeatView::position));
        FoundryPushResult guideResult = self.pushSessionGuideDocument(worldId, sessionId, session, beats);
        warnings.addAll(guideResult.warnings());

        return new FoundrySessionPushResult(articlesPushed, handoutsPushed, rollTablesPushed, cardDecksPushed,
                guideResult.foundryDocumentId(), beats.size(), warnings);
    }

    /** Builds and pushes the session's "Session Guide" JournalEntry — the session's own beats
     * in order, so the GM has the actual run-sheet inside Foundry, not just the referenced
     * content {@link #pushSession} pushes above. Package-private (not part of any use-case
     * interface — purely an internal step of {@code pushSession}) but still {@code
     * @Transactional} and called through {@link #self}, for the same reason every other
     * entity-specific push in this class is: so this document's own persistence (the push
     * record) gets a real, short-lived transaction rather than silently running with none. */
    @Transactional
    FoundryPushResult pushSessionGuideDocument(UUID worldId, UUID sessionId, SessionView session,
                                               List<ArcBeatView> beats) {
        Credentials credentials = credentialsFor(requireConnection(worldId));
        List<String> warnings = new ArrayList<>();

        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(guideTitle(session)).append("\n\n");
        if (session.summary() != null && !session.summary().isBlank()) {
            markdown.append(session.summary()).append("\n\n");
        }
        for (ArcBeatView beat : beats) {
            markdown.append("## ").append(beat.title()).append("\n\n");
            String body = beat.body();
            if (body != null && !body.isBlank()) {
                Set<String> targets = articleRenderer.linkTargets(body);
                Map<String, UUID> resolved = targets.isEmpty() ? Map.of() : articles.resolveRefs(worldId, targets);
                // Real clickable Foundry document links here (not the plain bold/italic every
                // other pushed document uses) — @UUID[...] is Foundry's own content-link
                // enricher syntax, same reference style already used for TableResult.documentUuid
                // (Phase 4), not independently confirmed against Foundry's official docs the way
                // other assumptions in this feature were; flag as unverified until live-tested.
                String linked = FoundryDocumentLinkRewriter.rewrite(body, resolved::get,
                        articleId -> StableFoundryId.from(documentKey(worldId, FoundryEntityType.ARTICLE, articleId)));
                markdown.append(linked).append("\n\n");
            }
            String references = referencesLine(worldId, beat);
            if (references != null) {
                markdown.append(references).append("\n\n");
            }
        }

        String folderId = upsertFolderFor(worldId, FoundryEntityType.SESSION_GUIDE, credentials);
        String rewrittenBody = uploadEmbeddedMedia(worldId, markdown.toString(), credentials, warnings);
        String documentId = StableFoundryId.from(documentKey(worldId, FoundryEntityType.SESSION_GUIDE, sessionId));
        // markdownToHtml's flexmark renderer HTML-entity-encodes bare "@" as part of its
        // autolink/email-obfuscation behavior (confirmed empirically: "@UUID[...]" comes back
        // as "&#64;UUID[...]") - harmless for every other pushed document (none of them contain
        // a literal "@"), but it would silently break every Foundry content-link in this guide,
        // since Foundry's @UUID[...] enricher matches the literal "@" character in the HTML
        // source, not its entity-encoded form. Reversing this one specific, self-generated
        // sequence is safe: nothing else in this feature ever emits "&#64;UUID[".
        String htmlBody = articleRenderer.markdownToHtml(rewrittenBody).replace("&#64;UUID[", "@UUID[");
        relay.upsertJournalEntry(credentials, documentId, guideTitle(session), rewrittenBody, htmlBody, folderId);
        recordPush(worldId, FoundryEntityType.SESSION_GUIDE, sessionId, documentId);

        return new FoundryPushResult(documentId, clock.instant(), warnings);
    }

    private static String guideTitle(SessionView session) {
        return session.sessionNumber() != null
                ? "Session " + session.sessionNumber() + ": " + session.title()
                : session.title();
    }

    /** A beat's own tagged article/table/deck references (distinct from any {@code [[wiki-link]]}
     * mentioned inline in its body) as a "**References:**" line of real Foundry document links —
     * statblock/encounter ids are deliberately never consulted here, permanently out of scope for
     * this feature. Omits a reference silently (rather than failing the whole push) if the tagged
     * entity no longer exists. Returns {@code null} (no line at all) when a beat tags nothing. */
    private String referencesLine(UUID worldId, ArcBeatView beat) {
        List<String> refs = new ArrayList<>();
        for (UUID articleId : beat.articleIds()) {
            articles.findByIdInWorld(articleId, worldId).ifPresent(a -> refs.add("@UUID[JournalEntry."
                    + StableFoundryId.from(documentKey(worldId, FoundryEntityType.ARTICLE, articleId)) + "]{"
                    + a.title() + "}"));
        }
        for (UUID tableId : beat.tableIds()) {
            rollTables.findByIdInWorld(tableId, worldId).ifPresent(t -> refs.add("@UUID[RollTable."
                    + StableFoundryId.from(documentKey(worldId, FoundryEntityType.ROLL_TABLE, tableId)) + "]{"
                    + t.title() + "}"));
        }
        for (UUID deckId : beat.deckIds()) {
            cardDecks.findByIdInWorld(deckId, worldId).ifPresent(d -> refs.add("@UUID[Cards."
                    + StableFoundryId.from(documentKey(worldId, FoundryEntityType.CARD_DECK, deckId)) + "]{"
                    + d.title() + "}"));
        }
        return refs.isEmpty() ? null : "**References:** " + String.join(", ", refs);
    }

    /** Bulk-runs {@link #pushSession} across every session in the campaign (ADR-0115 addendum).
     * Deliberately no {@code @Transactional} here for the same reason as {@link #pushSession}
     * itself: this method does no persistence of its own, only delegates through {@link #self}
     * so each session's own push keeps its own short transaction rather than one holding the
     * whole batch open. */
    @Override
    public FoundryCampaignPushResult pushCampaign(UUID worldId, UUID campaignId) {
        List<SessionView> campaignSessions = sessions.findOrdered(campaignId);
        List<String> warnings = new ArrayList<>();
        int articlesPushed = 0;
        int handoutsPushed = 0;
        int rollTablesPushed = 0;
        int cardDecksPushed = 0;
        int sessionGuidesCreated = 0;
        for (SessionView session : campaignSessions) {
            FoundrySessionPushResult result = self.pushSession(worldId, campaignId, session.id());
            articlesPushed += result.articlesPushed();
            handoutsPushed += result.handoutsPushed();
            rollTablesPushed += result.rollTablesPushed();
            cardDecksPushed += result.cardDecksPushed();
            if (result.sessionGuideDocumentId() != null) {
                sessionGuidesCreated++;
            }
            warnings.addAll(result.warnings());
        }
        return new FoundryCampaignPushResult(campaignSessions.size(), articlesPushed, handoutsPushed,
                rollTablesPushed, cardDecksPushed, sessionGuidesCreated, warnings);
    }

    @Override
    @Transactional
    public FoundryCategoryPushResult pushCategory(UUID worldId, UUID categoryId, FoundryCategoryPushMode mode) {
        if (!categories.existsInWorld(categoryId, worldId)) {
            throw new NotFoundException("Category not found");
        }
        Credentials credentials = credentialsFor(requireConnection(worldId));
        Map<UUID, CategoryView> byId = categoryMapFor(worldId);
        CategoryView rootCategory = byId.get(categoryId);
        Map<UUID, List<ArticleView>> articlesByCategory = articlesByCategoryFor(worldId);

        return mode == FoundryCategoryPushMode.FOLDER
                ? pushCategoryTreeAsFolders(worldId, List.of(rootCategory), categoryId, byId, articlesByCategory,
                        List.of(), credentials, FoundryEntityType.CATEGORY, categoryId)
                : pushCategoryTreeAsSingleDocument(worldId, rootCategory.name(), List.of(rootCategory), categoryId,
                        byId, articlesByCategory, List.of(), credentials, FoundryEntityType.CATEGORY, categoryId);
    }

    @Override
    @Transactional
    public FoundryCategoryPushResult pushWorldWiki(UUID worldId) {
        Credentials credentials = credentialsFor(requireConnection(worldId));
        Map<UUID, CategoryView> byId = categoryMapFor(worldId);
        List<CategoryView> topLevelCategories = byId.values().stream()
                .filter(c -> c.parentId() == null)
                .sorted(Comparator.comparing(CategoryView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<UUID, List<ArticleView>> articlesByCategory = articlesByCategoryFor(worldId);
        List<ArticleView> uncategorized = articles.findByWorld(worldId).stream()
                .filter(a -> a.categoryId() == null)
                .sorted(Comparator.comparing(ArticleView::title, String.CASE_INSENSITIVE_ORDER))
                .toList();

        // Always FOLDER — the single-document bundle is a category-scoped choice only (the user
        // explicitly didn't want a whole-wiki-as-one-document option).
        return pushCategoryTreeAsFolders(worldId, topLevelCategories, null, byId, articlesByCategory, uncategorized,
                credentials, FoundryEntityType.WIKI, worldId);
    }

    private Map<UUID, CategoryView> categoryMapFor(UUID worldId) {
        return categories.findByWorld(worldId).stream().collect(Collectors.toMap(CategoryView::id, c -> c));
    }

    private Map<UUID, List<ArticleView>> articlesByCategoryFor(UUID worldId) {
        return articles.findByWorld(worldId).stream()
                .filter(a -> a.categoryId() != null)
                .collect(Collectors.groupingBy(ArticleView::categoryId));
    }

    /** {@code roots} and all their descendants, each subtree emitted parent-first with children
     * alphabetised — the order both push modes below lay categories out in. */
    private static List<CategoryView> categoryTreeOrder(List<CategoryView> roots, Map<UUID, CategoryView> byId) {
        Map<UUID, List<CategoryView>> childrenByParent = byId.values().stream()
                .filter(c -> c.parentId() != null)
                .collect(Collectors.groupingBy(CategoryView::parentId));
        List<CategoryView> ordered = new ArrayList<>();
        for (CategoryView root : roots) {
            appendCategorySubtree(root, childrenByParent, ordered);
        }
        return ordered;
    }

    private static void appendCategorySubtree(CategoryView category, Map<UUID, List<CategoryView>> childrenByParent,
                                              List<CategoryView> out) {
        out.add(category);
        childrenByParent.getOrDefault(category.id(), List.of()).stream()
                .sorted(Comparator.comparing(CategoryView::name, String.CASE_INSENSITIVE_ORDER))
                .forEach(child -> appendCategorySubtree(child, childrenByParent, out));
    }

    /** Pushes {@code roots} (and everything under them) as nested Foundry folders, each
     * article as its own separate JournalEntry inside the matching subfolder — the FOLDER
     * mode of the category/world push addendum (ADR-0115). {@code virtualRootId} is the single
     * category the caller actually asked to push (its folder always nests directly under the
     * flat "Articles" folder, never under its real parent category, since that parent wasn't
     * part of the request) — {@code null} for a world push, where every top-level category
     * already has no parent and nests under "Articles" naturally. */
    private FoundryCategoryPushResult pushCategoryTreeAsFolders(UUID worldId, List<CategoryView> roots,
                                                                UUID virtualRootId, Map<UUID, CategoryView> byId,
                                                                Map<UUID, List<ArticleView>> articlesByCategory,
                                                                List<ArticleView> uncategorized,
                                                                Credentials credentials, FoundryEntityType trackingType,
                                                                UUID trackingId) {
        String topFolderId = upsertFolderFor(worldId, FoundryEntityType.ARTICLE, credentials);
        Map<UUID, String> folderIdByCategory = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        int articlesPushed = 0;

        for (CategoryView category : categoryTreeOrder(roots, byId)) {
            String folderId = upsertCategoryFolderChain(worldId, category.id(), virtualRootId, byId,
                    folderIdByCategory, topFolderId, credentials);
            for (ArticleView article : articlesByCategory.getOrDefault(category.id(), List.of())) {
                String markdownBody = articleRenderer.renderBodyAsMarkdown(worldId, article.body());
                FoundryPushResult result = pushDocument(worldId, FoundryEntityType.ARTICLE, article.id(),
                        article.title(), markdownBody, folderId, credentials);
                warnings.addAll(result.warnings());
                articlesPushed++;
            }
        }
        for (ArticleView article : uncategorized) {
            String markdownBody = articleRenderer.renderBodyAsMarkdown(worldId, article.body());
            FoundryPushResult result = pushDocument(worldId, FoundryEntityType.ARTICLE, article.id(),
                    article.title(), markdownBody, topFolderId, credentials);
            warnings.addAll(result.warnings());
            articlesPushed++;
        }

        String trackedFolderId = trackingType == FoundryEntityType.CATEGORY
                ? folderIdByCategory.get(trackingId)
                : topFolderId;
        recordPush(worldId, trackingType, trackingId, trackedFolderId);
        return new FoundryCategoryPushResult(trackedFolderId, clock.instant(), articlesPushed, warnings);
    }

    /** Idempotent upsert of the Foundry folder for one category, nesting it under its parent
     * category's own folder — except {@code categoryId == virtualRootId}, which always nests
     * directly under {@code topFolderId} instead of its real parent (see
     * {@link #pushCategoryTreeAsFolders}'s javadoc for why). Memoized in {@code cache} since
     * the same category can be an ancestor of many articles processed in the same push. */
    private String upsertCategoryFolderChain(UUID worldId, UUID categoryId, UUID virtualRootId,
                                             Map<UUID, CategoryView> byId, Map<UUID, String> cache,
                                             String topFolderId, Credentials credentials) {
        String cached = cache.get(categoryId);
        if (cached != null) {
            return cached;
        }
        CategoryView category = byId.get(categoryId);
        String parentFolderId = categoryId.equals(virtualRootId) || category.parentId() == null
                ? topFolderId
                : upsertCategoryFolderChain(worldId, category.parentId(), virtualRootId, byId, cache, topFolderId,
                        credentials);
        String folderId = StableFoundryId.from("campaign-organizer:" + worldId + ":folder:category:" + categoryId);
        relay.upsertFolder(credentials, folderId, category.name(), parentFolderId);
        cache.put(categoryId, folderId);
        return folderId;
    }

    /** Pushes {@code roots} (and everything under them) as one JournalEntry document named
     * {@code documentTitle}, one page per article — the SINGLE_DOCUMENT mode of the
     * category/world push addendum (ADR-0115). An article directly in a root category (i.e.
     * one of {@code roots} itself, not a descendant subcategory) keeps its plain title as the
     * page name; every other article's page name is prefixed with its own category's name so
     * the subcategory grouping stays visible in Foundry's flat page list. */
    private FoundryCategoryPushResult pushCategoryTreeAsSingleDocument(UUID worldId, String documentTitle,
                                                                       List<CategoryView> roots, UUID trackingId,
                                                                       Map<UUID, CategoryView> byId,
                                                                       Map<UUID, List<ArticleView>> articlesByCategory,
                                                                       List<ArticleView> uncategorized,
                                                                       Credentials credentials,
                                                                       FoundryEntityType trackingType,
                                                                       UUID recordEntityId) {
        Set<UUID> rootIds = roots.stream().map(CategoryView::id).collect(Collectors.toSet());
        List<String> warnings = new ArrayList<>();
        List<JournalPageData> pages = new ArrayList<>();
        String documentId = StableFoundryId.from(documentKey(worldId, trackingType, recordEntityId));

        for (CategoryView category : categoryTreeOrder(roots, byId)) {
            boolean isRoot = rootIds.contains(category.id());
            List<ArticleView> categoryArticles = articlesByCategory.getOrDefault(category.id(), List.of()).stream()
                    .sorted(Comparator.comparing(ArticleView::title, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (ArticleView article : categoryArticles) {
                String pageName = isRoot ? article.title() : category.name() + " / " + article.title();
                pages.add(pageFor(worldId, documentId, article, pageName, credentials, warnings));
            }
        }
        for (ArticleView article : uncategorized) {
            pages.add(pageFor(worldId, documentId, article, article.title(), credentials, warnings));
        }

        String folderId = upsertFolderFor(worldId, FoundryEntityType.ARTICLE, credentials);
        relay.upsertJournalEntryWithPages(credentials, documentId, documentTitle, pages, folderId);
        recordPush(worldId, trackingType, recordEntityId, documentId);

        return new FoundryCategoryPushResult(documentId, clock.instant(), pages.size(), warnings);
    }

    private JournalPageData pageFor(UUID worldId, String documentId, ArticleView article, String pageName,
                                    Credentials credentials, List<String> warnings) {
        String markdownBody = articleRenderer.renderBodyAsMarkdown(worldId, article.body());
        String rewrittenBody = uploadEmbeddedMedia(worldId, markdownBody, credentials, warnings);
        String htmlBody = articleRenderer.markdownToHtml(rewrittenBody);
        String pageId = StableFoundryId.from(documentId + ":page:" + article.id());
        return new JournalPageData(pageId, pageName, rewrittenBody, htmlBody);
    }

    private void recordPush(UUID worldId, FoundryEntityType type, UUID entityId, String documentId) {
        Instant now = clock.instant();
        FoundryPushRecord record = pushRecords.findByEntity(worldId, type, entityId)
                .map(r -> {
                    r.recordPush(documentId, now);
                    return r;
                })
                .orElseGet(() -> FoundryPushRecord.create(ids.newId(), worldId, type, entityId, documentId, now));
        pushRecords.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoundryPushStatusView> statusFor(UUID worldId, FoundryEntityType entityType, UUID entityId) {
        return pushRecords.findByEntity(worldId, entityType, entityId)
                .map(r -> new FoundryPushStatusView(r.getFoundryDocumentId(), r.getPushedAt()));
    }

    /** Embedded-media upload, journal-upsert, push-record upsert — the part of a push that's
     * identical for every entity type. Callers obtain the title/Markdown body their own way (an
     * article renders wiki-links first; a handout has no rendering step at all) and the
     * destination {@code folderId} their own way too (a lone article/handout push uses its
     * type's flat top-level folder; a category/world push places it in a per-category
     * subfolder instead — ADR-0115 addendum). */
    private FoundryPushResult pushDocument(UUID worldId, FoundryEntityType type, UUID entityId, String title,
                                           String markdownBody, String folderId, Credentials credentials) {
        List<String> warnings = new ArrayList<>();
        String rewrittenBody = uploadEmbeddedMedia(worldId, markdownBody, credentials, warnings);

        String documentId = StableFoundryId.from(documentKey(worldId, type, entityId));
        String htmlBody = articleRenderer.markdownToHtml(rewrittenBody);
        relay.upsertJournalEntry(credentials, documentId, title, rewrittenBody, htmlBody, folderId);
        recordPush(worldId, type, entityId, documentId);

        return new FoundryPushResult(documentId, clock.instant(), warnings);
    }

    private String upsertFolderFor(UUID worldId, FoundryEntityType type, Credentials credentials) {
        String folderId = StableFoundryId.from("campaign-organizer:" + worldId + ":folder:" + folderKey(type));
        relay.upsertFolder(credentials, folderId, folderName(type), null);
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

    /** CATEGORY/WIKI never reach {@link #upsertFolderFor} — a category/world push builds its
     * own bespoke per-category folder chain instead ({@link #upsertCategoryFolderChain}), so
     * those two constants have no flat top-level folder of their own to look up here. */
    private static String folderKey(FoundryEntityType type) {
        return switch (type) {
            case ARTICLE -> "articles";
            case HANDOUT -> "handouts";
            case ROLL_TABLE -> "rolltables";
            case CARD_DECK -> "carddecks";
            case SESSION_GUIDE -> "sessionguides";
            case CATEGORY, WIKI -> throw new IllegalArgumentException(type + " has no flat top-level folder");
        };
    }

    private static String folderName(FoundryEntityType type) {
        return switch (type) {
            case ARTICLE -> "Articles";
            case HANDOUT -> "Handouts";
            case ROLL_TABLE -> "Roll Tables";
            case CARD_DECK -> "Card Decks";
            case SESSION_GUIDE -> "Session Guides";
            case CATEGORY, WIKI -> throw new IllegalArgumentException(type + " has no flat top-level folder");
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
