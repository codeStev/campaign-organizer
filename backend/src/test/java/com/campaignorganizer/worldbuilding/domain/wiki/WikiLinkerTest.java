package com.campaignorganizer.worldbuilding.domain.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.worldbuilding.domain.wiki.WikiLinker.LinkRef;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for wiki-link resolution (ADR-0014). */
class WikiLinkerTest {

    @Test
    void extractsLowercasedLinkTargets() {
        assertThat(WikiLinker.linkTargets("See [[Goblin]] and [[Waterdeep|the city]]."))
                .containsExactlyInAnyOrder("goblin", "waterdeep");
    }

    @Test
    void resolvesKnownLinkToAnchor() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.render("See [[Goblin]].",
                Map.of("goblin", new LinkRef(id, "Goblin")));

        assertThat(out).contains("<a class=\"wiki-link\" data-article-id=\"" + id + "\"");
        assertThat(out).contains(">Goblin</a>");
    }

    @Test
    void resolvesUnknownLinkToBrokenSpan() {
        String out = WikiLinker.render("See [[Nowhere]].", Map.of());
        assertThat(out).contains("<span class=\"broken-link\">Nowhere</span>");
    }

    @Test
    void usesExplicitLabelWhenProvided() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.render("Visit [[Waterdeep|the city]].",
                Map.of("waterdeep", new LinkRef(id, "Waterdeep")));
        assertThat(out).contains(">the city</a>");
    }
}
