package com.campaignorganizer.interchange.foundry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test (ADR-0115) — no mocks needed. */
class MediaEmbedRewriterTest {

    @Test
    void mediaIdsIn_extractsEveryDistinctReferenceInFirstSeenOrder() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        String body = "![alt](/api/media/" + a + "/content) text ![two](/api/media/" + b + "/content)"
                + " again ![dup](/api/media/" + a + "/content)";

        assertThat(MediaEmbedRewriter.mediaIdsIn(body)).containsExactly(a, b);
    }

    @Test
    void mediaIdsIn_emptyWhenNoReferences() {
        assertThat(MediaEmbedRewriter.mediaIdsIn("just text")).isEmpty();
        assertThat(MediaEmbedRewriter.mediaIdsIn(null)).isEmpty();
    }

    @Test
    void rewrite_replacesEachReferenceUsingTheResolver() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        String body = "![a](/api/media/" + a + "/content) ![b](/api/media/" + b + "/content)";

        String out = MediaEmbedRewriter.rewrite(body,
                id -> id.equals(a) ? "campaign-organizer/w/a.png" : null);

        assertThat(out).isEqualTo("![a](campaign-organizer/w/a.png) ![b](/api/media/" + b + "/content)");
    }

    @Test
    void rewrite_leavesReferenceUntouchedWhenResolverReturnsNull() {
        UUID id = UUID.randomUUID();
        String body = "![a](/api/media/" + id + "/content)";

        assertThat(MediaEmbedRewriter.rewrite(body, mediaId -> null)).isEqualTo(body);
    }

    @Test
    void rewrite_nullBodyReturnsNull() {
        assertThat(MediaEmbedRewriter.rewrite(null, id -> "x")).isNull();
    }
}
