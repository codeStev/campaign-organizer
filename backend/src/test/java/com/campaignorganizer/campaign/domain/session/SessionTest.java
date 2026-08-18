package com.campaignorganizer.campaign.domain.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the session aggregate. */
class SessionTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        Session s = Session.create(UUID.randomUUID(), UUID.randomUUID(), "Session 1", 1,
                LocalDate.parse("2026-01-01"), "sum", "notes", T0);
        s.update("Session 1b", 2, LocalDate.parse("2026-01-08"), "sum2", "notes2", T1);

        assertThat(s.getTitle()).isEqualTo("Session 1b");
        assertThat(s.getSessionNumber()).isEqualTo(2);
        assertThat(s.getCreatedAt()).isEqualTo(T0);
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Session.create(UUID.randomUUID(), UUID.randomUUID(), " ", null,
                null, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}
