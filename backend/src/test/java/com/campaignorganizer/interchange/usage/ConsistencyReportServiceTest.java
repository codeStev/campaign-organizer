package com.campaignorganizer.interchange.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.interchange.usage.application.port.in.ConsistencyDtos.ArticleIssue;
import com.campaignorganizer.interchange.usage.application.port.in.ConsistencyDtos.ConsistencyReport;
import com.campaignorganizer.interchange.usage.application.port.in.GetConsistencyReportUseCase;
import com.campaignorganizer.interchange.usage.application.port.published.UsageQueryPort;
import com.campaignorganizer.interchange.usage.application.service.ConsistencyReportService;
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
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The report's three sections against mocked ports. Link extraction in the
 * mocks mirrors the real renderer's contract: lowercase [[target]] names.
 */
@ExtendWith(MockitoExtension.class)
class ConsistencyReportServiceTest {

    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[(.+?)]]");

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID arcId = UUID.randomUUID();

    @Mock
    private WorldQueryPort worlds;
    @Mock
    private ArticleQueryPort articles;
    @Mock
    private ArticleRenderPort renderer;
    @Mock
    private CampaignQueryPort campaigns;
    @Mock
    private ArcQueryPort arcs;
    @Mock
    private ArcBeatQueryPort beats;
    @Mock
    private RollTableQueryPort rollTables;
    @Mock
    private CardDeckQueryPort cardDecks;
    @Mock
    private UsageQueryPort campaignUsage;

    private GetConsistencyReportUseCase service;

    /** Titles the fake world contains, used by the resolveRefs stub. */
    private Map<String, UUID> knownArticles = Map.of();

    @BeforeEach
    void setUp() {
        lenient().when(worlds.exists(worldId)).thenReturn(true);
        lenient().when(campaigns.findByWorld(worldId))
                .thenReturn(List.of(new CampaignView(campaignId, worldId, "Main", null, null,
                        CampaignStatus.ACTIVE, null, null, Instant.EPOCH, Instant.EPOCH)));
        lenient().when(arcs.findByCampaign(campaignId))
                .thenReturn(List.of(new ArcView(arcId, campaignId, "Act I", null, null, 0,
                        Instant.EPOCH, Instant.EPOCH)));
        // Extract [[targets]] from the body like the real renderer does.
        lenient().when(renderer.linkTargets(any())).thenAnswer(inv -> {
            String body = inv.getArgument(0, String.class);
            if (body == null) {
                return Set.of();
            }
            return WIKI_LINK.matcher(body).results()
                    .map(m -> m.group(1).toLowerCase())
                    .collect(Collectors.toSet());
        });
        // Resolve a target iff it names a known article (slug or title).
        lenient().when(articles.resolveRefs(eq(worldId), anySet())).thenAnswer(inv -> {
            Set<String> names = inv.getArgument(1);
            return names.stream()
                    .filter(n -> knownArticles.containsKey(n)
                            || knownArticles.keySet().stream()
                                    .anyMatch(t -> t.toLowerCase().equals(n)))
                    .collect(Collectors.toMap(java.util.function.Function.identity(),
                            n -> knownArticles.getOrDefault(n,
                                    knownArticles.entrySet().stream()
                                            .filter(e -> e.getKey().toLowerCase().equals(n))
                                            .findFirst().orElseThrow().getValue())));
        });
        service = new ConsistencyReportService(worlds, articles, renderer, campaigns,
                arcs, beats, rollTables, cardDecks, campaignUsage);
    }

    private ArticleView article(String title, String body) {
        return article(title, body, null);
    }

    private ArticleView article(String title, String body, UUID parentArticleId) {
        UUID id = UUID.nameUUIDFromBytes(title.getBytes());
        return new ArticleView(id, worldId, null, parentArticleId, title, title.toLowerCase(), null,
                body, Instant.EPOCH, Instant.EPOCH);
    }

    private ArcBeatView beat(String title, String body, List<UUID> articleIds) {
        return new ArcBeatView(UUID.randomUUID(), arcId, title, body, false,
                articleIds, List.of(), List.of(), List.of(), List.of(), null, null, 0, Instant.EPOCH,
                Instant.EPOCH);
    }

    @Test
    void unknownWorld_isNotFound() {
        when(worlds.exists(worldId)).thenReturn(false);
        assertThatThrownBy(() -> service.report(worldId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void emptyWorld_reportsThreeEmptySections() {
        when(articles.findByWorld(worldId)).thenReturn(List.of());

        ConsistencyReport r = service.report(worldId);

        assertThat(r.brokenLinks()).isEmpty();
        assertThat(r.orphanedArticles()).isEmpty();
        assertThat(r.unreferencedByCampaigns()).isEmpty();
    }

    @Test
    void brokenLinks_areReportedPerSourceWithLabels() {
        ArticleView harbor = article("Harbor", "See [[Harbor Master]] and [[Missing Isle]].");
        when(articles.findByWorld(worldId)).thenReturn(List.of(harbor));
        when(beats.findByArc(arcId)).thenReturn(List.of(
                beat("Ambush", "Fight near the [[harbor master]] while [[the kraken]] rises.",
                        List.of())));
        when(rollTables.findByWorld(worldId)).thenReturn(List.of(new RollTableView(
                UUID.randomUUID(), worldId, null, "Weather", null, "1d6", 1, 6,
                List.of(new RollTableEntryView(UUID.randomUUID(), null, null, "[[Storm]] hits.",
                        List.of(), List.of())),
                Instant.EPOCH, Instant.EPOCH)));
        when(cardDecks.findByWorld(worldId)).thenReturn(List.of(new CardDeckView(
                UUID.randomUUID(), worldId, null, "Rumors", null,
                List.of(new DeckCardView(UUID.randomUUID(), "R1", "[[Ghost]] seen.", List.of(), List.of())),
                Instant.EPOCH, Instant.EPOCH)));
        knownArticles = Map.of("Harbor Master", UUID.randomUUID());

        ConsistencyReport r = service.report(worldId);

        assertThat(r.brokenLinks()).extracting(d -> d.sourceType() + "|" + d.target())
                .containsExactlyInAnyOrder(
                        "ARTICLE|missing isle",
                        "BEAT|the kraken",
                        "ROLL_TABLE|storm",
                        "CARD_DECK|ghost");
        // Resolved links (the beat's [[harbor]]) are not reported broken.
        assertThat(r.brokenLinks()).noneMatch(d -> d.target().equals("harbor"));
        assertThat(r.brokenLinks()).filteredOn(d -> d.sourceType().equals("BEAT"))
                .allSatisfy(d -> assertThat(d.sourceLabel()).contains("Act I").contains("Ambush"));
    }

    @Test
    void orphans_considerArticleLinksBeatRefs_andIgnoreSelfLinks() {
        ArticleView a = article("A", "Points at [[b]].");
        ArticleView b = article("B", "Plain.");
        ArticleView c = article("C", "Plain.");
        ArticleView d = article("D", "[[d]]"); // links only itself
        when(articles.findByWorld(worldId)).thenReturn(List.of(a, b, c, d));
        when(beats.findByArc(arcId)).thenReturn(List.of(beat("B1", "", List.of(c.id()))));
        when(rollTables.findByWorld(worldId)).thenReturn(List.of());
        when(cardDecks.findByWorld(worldId)).thenReturn(List.of());
        knownArticles = Map.of("A", a.id(), "B", b.id(), "C", c.id(), "D", d.id());

        ConsistencyReport r = service.report(worldId);

        // B is linked by A; C by the beat's explicit reference. A and D are orphans.
        assertThat(r.orphanedArticles()).extracting(ArticleIssue::title)
                .containsExactlyInAnyOrder("A", "D");
    }

    @Test
    void childWithParent_isNotOrphaned_evenWithNoProseLinkOrBeatRef() {
        ArticleView parent = article("Sunken Temple", "Plain.");
        ArticleView child = article("Flooded Corridor", "Plain.", parent.id());
        when(articles.findByWorld(worldId)).thenReturn(List.of(parent, child));
        when(beats.findByArc(arcId)).thenReturn(List.of());
        when(rollTables.findByWorld(worldId)).thenReturn(List.of());
        when(cardDecks.findByWorld(worldId)).thenReturn(List.of());
        knownArticles = Map.of("Sunken Temple", parent.id(), "Flooded Corridor", child.id());

        ConsistencyReport r = service.report(worldId);

        // The child is reachable via the parent's sidebar nesting/Used-by
        // panel, so it's rescued from orphan status; the parent itself has
        // no inbound reference of any kind and is still orphaned.
        assertThat(r.orphanedArticles()).extracting(ArticleIssue::title)
                .containsExactly("Sunken Temple");
    }

    @Test
    void campaignUnreferenced_comesFromTheUsagePort_notFromProseLinks() {
        ArticleView a = article("A", "[[b]]");   // prose-linked but no campaign path
        ArticleView b = article("B", "");
        when(articles.findByWorld(worldId)).thenReturn(List.of(a, b));
        when(beats.findByArc(arcId)).thenReturn(List.of());
        when(rollTables.findByWorld(worldId)).thenReturn(List.of());
        when(cardDecks.findByWorld(worldId)).thenReturn(List.of());
        when(campaignUsage.articleIdsUsedInCampaign(worldId, campaignId)).thenReturn(Set.of(b.id()));
        knownArticles = Map.of("A", a.id(), "B", b.id());

        ConsistencyReport r = service.report(worldId);

        assertThat(r.unreferencedByCampaigns()).extracting(ArticleIssue::title)
                .containsExactly("A");
        // Prose links do not count as campaign usage: B is linked by A, so only
        // A is an orphan — but neither article reaches a campaign.
        assertThat(r.orphanedArticles()).extracting(ArticleIssue::title).containsExactly("A");
    }
}
