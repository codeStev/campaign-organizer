package com.campaignorganizer.ai.application.port.out;

import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.List;
import java.util.UUID;

public interface AiProviderSettingsRepositoryPort {

    /** Empty if nothing has ever been saved for this account. */
    List<ProviderSetting> findAllOrderedByPriority(UUID ownerId);

    /** Replaces every row owned by this account; the small, fixed provider set makes this simpler than upserting. */
    void replaceAll(UUID ownerId, List<ProviderSetting> settings);

    /** One-time backfill only (ADR-0109). */
    void assignUnownedTo(UUID ownerId);
}
