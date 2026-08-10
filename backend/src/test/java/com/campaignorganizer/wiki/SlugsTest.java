package com.campaignorganizer.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void slugifyLowercasesAndHyphenates() {
        assertThat(Slugs.slugify("The Grand Bazaar")).isEqualTo("the-grand-bazaar");
    }

    @Test
    void slugifyStripsAccentsAndPunctuation() {
        assertThat(Slugs.slugify("Café del Río!!")).isEqualTo("cafe-del-rio");
    }

    @Test
    void slugifyFallsBackForEmptyResult() {
        assertThat(Slugs.slugify("***")).isEqualTo("untitled");
    }

    @Test
    void deduplicateReturnsBaseWhenFree() {
        assertThat(Slugs.deduplicate("goblin", s -> false)).isEqualTo("goblin");
    }

    @Test
    void deduplicateAppendsNumericSuffix() {
        Set<String> taken = Set.of("goblin", "goblin-2");
        assertThat(Slugs.deduplicate("goblin", taken::contains)).isEqualTo("goblin-3");
    }
}
