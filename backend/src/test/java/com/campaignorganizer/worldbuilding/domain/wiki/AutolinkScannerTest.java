package com.campaignorganizer.worldbuilding.domain.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.worldbuilding.domain.wiki.AutolinkScanner.Candidate;
import com.campaignorganizer.worldbuilding.domain.wiki.AutolinkScanner.Occurrence;
import com.campaignorganizer.worldbuilding.domain.wiki.AutolinkScanner.Selection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the auto-link scan/apply (ADR-0116). */
class AutolinkScannerTest {

    @Test
    void findsUnlinkedMentionOfAnotherArticlesTitle() {
        UUID goblinId = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(goblinId, List.of("Goblin")));

        List<Occurrence> found = AutolinkScanner.scan("A goblin approaches.", UUID.randomUUID(), candidates);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).targetArticleId()).isEqualTo(goblinId);
        assertThat(found.get(0).matchedText()).isEqualTo("goblin");
        assertThat(found.get(0).occurrenceIndex()).isEqualTo(0);
    }

    @Test
    void skipsSelfMentions() {
        UUID selfId = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(selfId, List.of("Goblin")));

        assertThat(AutolinkScanner.scan("Another goblin.", selfId, candidates)).isEmpty();
    }

    @Test
    void skipsMentionsAlreadyInsideAWikiLink() {
        UUID goblinId = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(goblinId, List.of("Goblin")));

        assertThat(AutolinkScanner.scan("A [[Goblin]] approaches.", UUID.randomUUID(), candidates)).isEmpty();
    }

    @Test
    void skipsMentionsInsideInlineCode() {
        UUID goblinId = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(goblinId, List.of("Goblin")));

        assertThat(AutolinkScanner.scan("Variable `goblin` in code.", UUID.randomUUID(), candidates)).isEmpty();
    }

    @Test
    void skipsNamesShorterThanMinimumLength() {
        UUID id = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(id, List.of("Oz")));

        assertThat(AutolinkScanner.scan("Oz is here.", UUID.randomUUID(), candidates)).isEmpty();
    }

    @Test
    void longestOverlappingMatchWinsAtAGivenPosition() {
        UUID waterdeepId = UUID.randomUUID();
        UUID docksId = UUID.randomUUID();
        List<Candidate> candidates = List.of(
                new Candidate(waterdeepId, List.of("Waterdeep")),
                new Candidate(docksId, List.of("North Waterdeep Docks")));

        List<Occurrence> found = AutolinkScanner.scan("We reach North Waterdeep Docks at dawn.",
                UUID.randomUUID(), candidates);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).targetArticleId()).isEqualTo(docksId);
        assertThat(found.get(0).matchedText()).isEqualTo("North Waterdeep Docks");
    }

    @Test
    void candidateNamesOfferEveryKnownNameFormRegardlessOfWhichOneMatched() {
        UUID id = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(id, List.of("Robert Gutkind", "Bob", "Jasper")));

        List<Occurrence> found = AutolinkScanner.scan("Talked to Bob today.", UUID.randomUUID(), candidates);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).candidateNames()).containsExactly("Robert Gutkind", "Bob", "Jasper");
    }

    @Test
    void occurrenceIndexCountsPerTargetInDocumentOrder() {
        UUID goblinId = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(goblinId, List.of("Goblin")));

        List<Occurrence> found = AutolinkScanner.scan("A goblin, then another goblin.", UUID.randomUUID(), candidates);

        assertThat(found).hasSize(2);
        assertThat(found.get(0).occurrenceIndex()).isEqualTo(0);
        assertThat(found.get(1).occurrenceIndex()).isEqualTo(1);
    }

    @Test
    void applyConvertsOnlySelectedOccurrenceUsingLabeledFormToPreserveOriginalWording() {
        UUID goblinId = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(goblinId, List.of("Goblin")));
        Set<Selection> selected = Set.of(new Selection(goblinId, 1, "Goblin"));

        String out = AutolinkScanner.apply("A goblin, then another goblin.", UUID.randomUUID(), candidates, selected);

        assertThat(out).isEqualTo("A goblin, then another [[Goblin|goblin]].");
    }

    @Test
    void applyUsesChosenAliasAsTargetInsteadOfWhicheverFormMatched() {
        UUID id = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(id, List.of("Robert Gutkind", "Bob")));
        Set<Selection> selected = Set.of(new Selection(id, 0, "Robert Gutkind"));

        String out = AutolinkScanner.apply("Talked to Bob today.", UUID.randomUUID(), candidates, selected);

        assertThat(out).isEqualTo("Talked to [[Robert Gutkind|Bob]] today.");
    }

    @Test
    void applyLeavesUnselectedOccurrencesOfTheSameTargetUntouched() {
        UUID goblinId = UUID.randomUUID();
        List<Candidate> candidates = List.of(new Candidate(goblinId, List.of("Goblin")));
        Set<Selection> selected = Set.of(new Selection(goblinId, 0, "Goblin"));

        String out = AutolinkScanner.apply("A goblin, then another goblin.", UUID.randomUUID(), candidates, selected);

        assertThat(out).isEqualTo("A [[Goblin|goblin]], then another goblin.");
    }
}
