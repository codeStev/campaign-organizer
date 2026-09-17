package com.campaignorganizer.interchange.foundry.adapter.out.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link FoundryRelayAdapter}'s JournalEntryPage-building logic — no HTTP,
 * mirroring {@code StableFoundryIdTest}'s style. Regression coverage for a real bug found via
 * live testing (ADR-0115): the page map originally had no {@code _id}, so Foundry's
 * embedded-collection upsert-by-id semantics treated every re-push as a brand-new page to add,
 * accumulating a duplicate page per push instead of replacing the existing one.
 */
class FoundryRelayAdapterTest {

    @Test
    void buildPage_assignsAStableId() {
        Map<String, Object> page = FoundryRelayAdapter.buildPage("doc123456789abc", "My Article", "**md**",
                "<p>md</p>");

        assertThat(page.get("_id")).isNotNull();
        assertThat((String) page.get("_id")).matches("^[A-Za-z0-9]{16}$");
    }

    @Test
    void buildPage_idIsDeterministicAcrossRepeatedPushesOfTheSameDocument() {
        Map<String, Object> first = FoundryRelayAdapter.buildPage("doc123456789abc", "My Article", "**md**",
                "<p>md</p>");
        Map<String, Object> second = FoundryRelayAdapter.buildPage("doc123456789abc", "My Article", "**md v2**",
                "<p>md v2</p>");

        assertThat(second.get("_id")).isEqualTo(first.get("_id"));
    }

    @Test
    void buildPage_idDiffersAcrossDifferentDocuments() {
        Map<String, Object> articlePage = FoundryRelayAdapter.buildPage("doc123456789abc", "Article", "a", "<p>a</p>");
        Map<String, Object> handoutPage = FoundryRelayAdapter.buildPage("doc987654321zyx", "Handout", "a", "<p>a</p>");

        assertThat(articlePage.get("_id")).isNotEqualTo(handoutPage.get("_id"));
    }

    @Test
    void buildPage_carriesNameTypeAndBothTextFormats() {
        Map<String, Object> page = FoundryRelayAdapter.buildPage("doc123456789abc", "My Article", "**bold**",
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
