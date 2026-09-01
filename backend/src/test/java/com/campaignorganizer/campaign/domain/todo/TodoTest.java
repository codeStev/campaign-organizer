package com.campaignorganizer.campaign.domain.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the todo aggregate (ADR-0092). */
class TodoTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void createsAStandingTodoWithNullSessionId() {
        Todo t = Todo.create(UUID.randomUUID(), UUID.randomUUID(), null, "Print handout X", false, T0);

        assertThat(t.getSessionId()).isNull();
        assertThat(t.isDone()).isFalse();
        assertThat(t.getCreatedAt()).isEqualTo(T0);
        assertThat(t.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void createsASessionTodoWithSessionId() {
        UUID sessionId = UUID.randomUUID();
        Todo t = Todo.create(UUID.randomUUID(), UUID.randomUUID(), sessionId, "Reskin the goblin", false, T0);

        assertThat(t.getSessionId()).isEqualTo(sessionId);
    }

    @Test
    void updateBumpsUpdatedAtAndTogglesDone() {
        Todo t = Todo.create(UUID.randomUUID(), UUID.randomUUID(), null, "Update faction standings", false,
                T0);

        t.update("Update faction standings (done)", true, T1);

        assertThat(t.getText()).isEqualTo("Update faction standings (done)");
        assertThat(t.isDone()).isTrue();
        assertThat(t.getUpdatedAt()).isEqualTo(T1);
        assertThat(t.getCreatedAt()).isEqualTo(T0);
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> Todo.create(UUID.randomUUID(), UUID.randomUUID(), null, " ", false, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsOverlongText() {
        String tooLong = "x".repeat(2001);
        assertThatThrownBy(() -> Todo.create(UUID.randomUUID(), UUID.randomUUID(), null, tooLong, false, T0))
                .isInstanceOf(ValidationException.class);
    }
}
