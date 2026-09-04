package com.campaignorganizer.campaign.domain.arc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the arc + beat aggregates. */
class ArcTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void arcUpdateBumpsUpdatedAt() {
        Arc a = Arc.create(UUID.randomUUID(), UUID.randomUUID(), "Act I", "desc", ArcStatus.ACTIVE, 1, T0);
        a.update("Act II", "desc2", ArcStatus.COMPLETED, 2, T1);

        assertThat(a.getTitle()).isEqualTo("Act II");
        assertThat(a.getStatus()).isEqualTo(ArcStatus.COMPLETED);
        assertThat(a.getCreatedAt()).isEqualTo(T0);
        assertThat(a.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void arcNullStatusDefaultsToPlanned() {
        Arc a = Arc.create(UUID.randomUUID(), UUID.randomUUID(), "Act I", null, null, 0, T0);
        assertThat(a.getStatus()).isEqualTo(ArcStatus.PLANNED);
    }

    @Test
    void arcRejectsBlankTitle() {
        assertThatThrownBy(() ->
                Arc.create(UUID.randomUUID(), UUID.randomUUID(), " ", null, ArcStatus.PLANNED, 0, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void beatNullLinkCollectionsBecomeEmpty() {
        ArcBeat b = ArcBeat.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null,
                null, null, "Ambush", "body", false, 0, T0);
        assertThat(b.getArticleIds()).isEmpty();
        assertThat(b.getStatblockIds()).isEmpty();
        assertThat(b.getEncounterIds()).isEmpty();
        assertThat(b.getTableIds()).isEmpty();
        assertThat(b.getDeckIds()).isEmpty();
    }

    @Test
    void beatUpdateReplacesLinksAndBumpsUpdatedAt() {
        ArcBeat b = ArcBeat.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null,
                null, null, "Ambush", "body", false, 0, T0);
        UUID article = UUID.randomUUID();
        b.update(List.of(article), null, null, null, null, null, null, "Ambush!", "body2", true, 1, T1);

        assertThat(b.getArticleIds()).containsExactly(article);
        assertThat(b.isDone()).isTrue();
        assertThat(b.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void beatRejectsBlankTitle() {
        assertThatThrownBy(() -> ArcBeat.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                null, null, null, null, " ", "body", false, 0, T0))
                .isInstanceOf(ValidationException.class);
    }
}
