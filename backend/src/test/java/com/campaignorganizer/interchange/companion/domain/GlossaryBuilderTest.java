package com.campaignorganizer.interchange.companion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryBuilder;
import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryTerm;
import com.campaignorganizer.interchange.companion.domain.glossary.GlossaryTermKind;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit coverage for the glossary selection and merge rules (ADR-0120). */
class GlossaryBuilderTest {

    @Test
    void sortsByNameIgnoringCase() {
        List<GlossaryTerm> terms = new GlossaryBuilder()
                .add("rabenfels", GlossaryTermKind.LOCATION, List.of())
                .add("Eckart", GlossaryTermKind.CHARACTER, List.of())
                .add("Baron Voss", GlossaryTermKind.CHARACTER, List.of())
                .build();

        assertThat(terms).extracting(GlossaryTerm::name).containsExactly("Baron Voss", "Eckart", "rabenfels");
    }

    @Test
    void mergesSameNameAndKindCaseInsensitivelyAndCombinesAliases() {
        List<GlossaryTerm> terms = new GlossaryBuilder()
                .add("Eckart", GlossaryTermKind.CHARACTER, List.of("Der Wirt"))
                .add("eckart", GlossaryTermKind.CHARACTER, List.of("Gastwirt Eckart", "der wirt"))
                .build();

        assertThat(terms).hasSize(1);
        assertThat(terms.get(0).name()).isEqualTo("Eckart");
        assertThat(terms.get(0).aliases()).containsExactly("Der Wirt", "Gastwirt Eckart");
    }

    @Test
    void keepsTheSameNameUnderDifferentKinds() {
        List<GlossaryTerm> terms = new GlossaryBuilder()
                .add("Wolf", GlossaryTermKind.CREATURE, List.of())
                .add("Wolf", GlossaryTermKind.ORGANIZATION, List.of())
                .build();

        assertThat(terms).extracting(GlossaryTerm::kind)
                .containsExactly(GlossaryTermKind.ORGANIZATION, GlossaryTermKind.CREATURE);
    }

    @Test
    void dropsBlankNamesBlankAliasesAndAliasesEqualToTheName() {
        List<GlossaryTerm> terms = new GlossaryBuilder()
                .add("  ", GlossaryTermKind.ITEM, List.of())
                .add(null, GlossaryTermKind.ITEM, null)
                .add("Sunblade", GlossaryTermKind.ITEM, java.util.Arrays.asList("sunblade", " ", null, "Sonnenklinge"))
                .build();

        assertThat(terms).hasSize(1);
        assertThat(terms.get(0).aliases()).containsExactly("Sonnenklinge");
    }

    @Test
    void onlyProperNounTemplatesBecomeArticleKinds() {
        assertThat(GlossaryTermKind.fromArticleTemplate("CHARACTER")).contains(GlossaryTermKind.CHARACTER);
        assertThat(GlossaryTermKind.fromArticleTemplate("LOCATION")).contains(GlossaryTermKind.LOCATION);
        assertThat(GlossaryTermKind.fromArticleTemplate("ORGANIZATION")).contains(GlossaryTermKind.ORGANIZATION);
        assertThat(GlossaryTermKind.fromArticleTemplate("SPECIES")).contains(GlossaryTermKind.SPECIES);
        assertThat(GlossaryTermKind.fromArticleTemplate("ITEM")).contains(GlossaryTermKind.ITEM);
        assertThat(GlossaryTermKind.fromArticleTemplate("GENERIC")).isEmpty();
        assertThat(GlossaryTermKind.fromArticleTemplate("EVENT")).isEmpty();
        assertThat(GlossaryTermKind.fromArticleTemplate(null)).isEmpty();
        assertThat(GlossaryTermKind.fromArticleTemplate("SOMETHING_NEW")).isEmpty();
    }

    @Test
    void wireValuesAreLowercase() {
        assertThat(GlossaryTermKind.CREATURE.wireValue()).isEqualTo("creature");
    }
}
