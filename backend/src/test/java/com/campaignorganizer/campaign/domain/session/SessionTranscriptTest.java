package com.campaignorganizer.campaign.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for transcripts (ADR-0121): only story lines, bounded, replaced as a whole. */
class SessionTranscriptTest {

    private static final Instant T0 = Instant.parse("2026-10-23T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-23T11:00:00Z");

    private static TranscriptLine line(String text) {
        return new TranscriptLine(1000, "Eckart", TranscriptLineKind.IN_CHARACTER, text);
    }

    @Test
    void onlyStoryKindsExist() {
        assertThat(TranscriptLineKind.values())
                .containsExactly(TranscriptLineKind.IN_CHARACTER, TranscriptLineKind.NARRATION);
    }

    @Test
    void parsesKindsAndRejectsEverythingElse() {
        assertThat(TranscriptLineKind.parse("NARRATION")).isEqualTo(TranscriptLineKind.NARRATION);
        for (String rejected : new String[] {"META", "OFF_TOPIC", "UNCLEAR", "narration", "", null}) {
            assertThatThrownBy(() -> TranscriptLineKind.parse(rejected)).isInstanceOf(ValidationException.class);
        }
    }

    @Test
    void aLineNormalisesABlankSpeakerToNone() {
        assertThat(new TranscriptLine(0, "  ", TranscriptLineKind.NARRATION, "Der Wind weht.").speaker()).isNull();
        assertThat(new TranscriptLine(0, " Eckart ", TranscriptLineKind.IN_CHARACTER, "Hallo").speaker())
                .isEqualTo("Eckart");
    }

    @Test
    void aLineRejectsBlankOrOverlongTextAndBadOffsetsAndLongSpeakers() {
        assertThatThrownBy(() -> line(" ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> line("x".repeat(TranscriptLine.MAX_TEXT_LENGTH + 1)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TranscriptLine(-1, null, TranscriptLineKind.NARRATION, "x"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TranscriptLine(0, "s".repeat(TranscriptLine.MAX_SPEAKER_LENGTH + 1), TranscriptLineKind.NARRATION, "x"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new TranscriptLine(0, null, null, "x")).isInstanceOf(ValidationException.class);
    }

    @Test
    void replacingKeepsTheCreationTimeAndBumpsTheUpdateTime() {
        SessionTranscript t = SessionTranscript.create(UUID.randomUUID(), UUID.randomUUID(), List.of(line("a")), T0);

        t.replaceLines(List.of(line("b"), line("c")), T1);

        assertThat(t.getLines()).extracting(TranscriptLine::text).containsExactly("b", "c");
        assertThat(t.getCreatedAt()).isEqualTo(T0);
        assertThat(t.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void aTranscriptNeedsLinesAndHasAnUpperBound() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> SessionTranscript.create(id, id, List.of(), T0))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> SessionTranscript.create(id, id, null, T0)).isInstanceOf(ValidationException.class);
        List<TranscriptLine> tooMany = IntStream.rangeClosed(0, SessionTranscript.MAX_LINES)
                .mapToObj(i -> line("l" + i)).toList();
        assertThatThrownBy(() -> SessionTranscript.create(id, id, tooMany, T0))
                .isInstanceOf(ValidationException.class);
    }
}
