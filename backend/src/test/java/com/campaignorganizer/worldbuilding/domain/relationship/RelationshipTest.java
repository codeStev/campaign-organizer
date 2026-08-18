package com.campaignorganizer.worldbuilding.domain.relationship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test. */
class RelationshipTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void createStampsTimestamps() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        Relationship r = Relationship.create(UUID.randomUUID(), UUID.randomUUID(), a, b, "knows", true, T0);

        assertThat(r.getFromArticleId()).isEqualTo(a);
        assertThat(r.getToArticleId()).isEqualTo(b);
        assertThat(r.isDirected()).isTrue();
        assertThat(r.getCreatedAt()).isEqualTo(T0);
        assertThat(r.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void updateBumpsUpdatedAtOnly() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        Relationship r = Relationship.create(UUID.randomUUID(), UUID.randomUUID(), a, b, "x", true, T0);

        r.update(a, c, "y", false, T1);

        assertThat(r.getToArticleId()).isEqualTo(c);
        assertThat(r.isDirected()).isFalse();
        assertThat(r.getCreatedAt()).isEqualTo(T0);
        assertThat(r.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void rejectsSelfRelation() {
        UUID a = UUID.randomUUID();
        assertThatThrownBy(() ->
                Relationship.create(UUID.randomUUID(), UUID.randomUUID(), a, a, null, true, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullEndpoint() {
        assertThatThrownBy(() ->
                Relationship.create(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), null, true, T0))
                .isInstanceOf(ValidationException.class);
    }
}
