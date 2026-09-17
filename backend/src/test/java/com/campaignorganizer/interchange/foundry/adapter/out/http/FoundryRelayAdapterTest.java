package com.campaignorganizer.interchange.foundry.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link FoundryRelayAdapter}'s JournalEntryPage-building logic — no HTTP,
 * mirroring {@code StableFoundryIdTest}'s style. Regression coverage for a real bug found via
 * live testing (ADR-0115): the page map originally had no {@code _id}, so Foundry's
 * embedded-collection upsert-by-id semantics treated every re-push as a brand-new page to add,
 * accumulating a duplicate page per push instead of replacing the existing one. {@code buildPage}
 * itself no longer hashes anything — callers ({@code upsertJournalEntry},
 * {@code upsertJournalEntryWithPages}) compute the final stable id and pass it in directly, so
 * id-determinism itself is {@code StableFoundryIdTest}'s concern, not this one's.
 */
class FoundryRelayAdapterTest {

    @Test
    void buildPage_carriesTheGivenIdVerbatim() {
        Map<String, Object> page = FoundryRelayAdapter.buildPage("abc123XYZ0123456", "My Article", "**md**",
                "<p>md</p>");

        assertThat(page.get("_id")).isEqualTo("abc123XYZ0123456");
    }

    @Test
    void buildPage_carriesNameTypeAndBothTextFormats() {
        Map<String, Object> page = FoundryRelayAdapter.buildPage("abc123XYZ0123456", "My Article", "**bold**",
                "<p><strong>bold</strong></p>");

        assertThat(page.get("name")).isEqualTo("My Article");
        assertThat(page.get("type")).isEqualTo("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) page.get("text");
        assertThat(text.get("format")).isEqualTo(2);
        assertThat(text.get("markdown")).isEqualTo("**bold**");
        assertThat(text.get("content")).isEqualTo("<p><strong>bold</strong></p>");
    }
}
