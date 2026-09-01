package com.campaignorganizer.campaign.domain.campaign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the campaign aggregate. */
class CampaignTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        UUID systemId = UUID.randomUUID();
        Campaign c = Campaign.create(UUID.randomUUID(), UUID.randomUUID(), "Rise of the Dragon",
                "desc", "notes", CampaignStatus.ACTIVE, systemId, T0);
        c.update("Fall of the Dragon", "desc2", "notes2", CampaignStatus.ON_HIATUS, systemId, T1);

        assertThat(c.getName()).isEqualTo("Fall of the Dragon");
        assertThat(c.getNotes()).isEqualTo("notes2");
        assertThat(c.getStatus()).isEqualTo(CampaignStatus.ON_HIATUS);
        assertThat(c.getSystemId()).isEqualTo(systemId);
        assertThat(c.getCreatedAt()).isEqualTo(T0);
        assertThat(c.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void defaultsStatusToPlannedWhenNull() {
        Campaign c = Campaign.create(UUID.randomUUID(), UUID.randomUUID(), "Rise of the Dragon",
                "desc", "notes", null, null, T0);

        assertThat(c.getStatus()).isEqualTo(CampaignStatus.PLANNED);
    }

    @Test
    void systemIdIsNullableAndUnvalidated() {
        Campaign c = Campaign.create(UUID.randomUUID(), UUID.randomUUID(), "Rise of the Dragon",
                "desc", "notes", CampaignStatus.ACTIVE, null, T0);

        assertThat(c.getSystemId()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() ->
                Campaign.create(UUID.randomUUID(), UUID.randomUUID(), " ", null, null, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}
