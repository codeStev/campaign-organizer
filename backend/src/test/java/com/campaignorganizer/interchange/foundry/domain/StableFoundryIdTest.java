package com.campaignorganizer.interchange.foundry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure domain unit test (ADR-0115) — no mocks needed. */
class StableFoundryIdTest {

    @Test
    void sameKeyAlwaysProducesTheSameId() {
        String key = "campaign-organizer:world1:article:art1";
        assertThat(StableFoundryId.from(key)).isEqualTo(StableFoundryId.from(key));
    }

    @Test
    void differentKeysProduceDifferentIds() {
        assertThat(StableFoundryId.from("a")).isNotEqualTo(StableFoundryId.from("b"));
    }

    @Test
    void nearIdenticalKeysProduceUnrelatedLookingIds() {
        String id1 = StableFoundryId.from("campaign-organizer:world:article:00000000-0000-0000-0000-000000000001");
        String id2 = StableFoundryId.from("campaign-organizer:world:article:00000000-0000-0000-0000-000000000002");
        assertThat(id1).isNotEqualTo(id2);
        // Avalanche sanity check: a one-character key difference shouldn't produce a
        // merely-shifted or partially-matching id.
        assertThat(id1.substring(0, 4)).isNotEqualTo(id2.substring(0, 4));
    }

    @Test
    void producesExactlySixteenAlphanumericCharacters() {
        assertThat(StableFoundryId.from("any-key")).matches("^[A-Za-z0-9]{16}$");
    }
}
