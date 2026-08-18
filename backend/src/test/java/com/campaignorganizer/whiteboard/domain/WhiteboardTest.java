package com.campaignorganizer.whiteboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test — no Spring, no database. */
class WhiteboardTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void createStampsBothTimestampsAndCopiesContents() {
        Whiteboard board = Whiteboard.create(UUID.randomUUID(), UUID.randomUUID(), "Plot",
                List.of(new Node("n1", "Hook", 0, 0, null)),
                List.of(new Edge("e1", "n1", "n1", "loop")), T0);

        assertThat(board.getCreatedAt()).isEqualTo(T0);
        assertThat(board.getUpdatedAt()).isEqualTo(T0);
        assertThat(board.getNodes()).hasSize(1);
        assertThat(board.getEdges()).hasSize(1);
    }

    @Test
    void reviseChangesContentAndBumpsUpdatedAtOnly() {
        Whiteboard board = Whiteboard.create(UUID.randomUUID(), UUID.randomUUID(), "A",
                List.of(), List.of(), T0);

        board.revise("B", List.of(new Node("n1", "x", 1, 2, "#fff")), List.of(), T1);

        assertThat(board.getName()).isEqualTo("B");
        assertThat(board.getNodes()).hasSize(1);
        assertThat(board.getCreatedAt()).isEqualTo(T0);
        assertThat(board.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void nullContentsBecomeEmptyLists() {
        Whiteboard board = Whiteboard.create(UUID.randomUUID(), UUID.randomUUID(), "A",
                null, null, T0);

        assertThat(board.getNodes()).isEmpty();
        assertThat(board.getEdges()).isEmpty();
    }

    @Test
    void blankNameIsRejected() {
        assertThatThrownBy(() -> Whiteboard.create(UUID.randomUUID(), UUID.randomUUID(), "  ",
                null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}
