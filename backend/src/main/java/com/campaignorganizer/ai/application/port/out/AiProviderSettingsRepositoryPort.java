package com.campaignorganizer.ai.application.port.out;

import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.List;

public interface AiProviderSettingsRepositoryPort {

    /** Empty if nothing has ever been saved. */
    List<ProviderSetting> findAllOrderedByPriority();

    /** Replaces every row; the small, fixed provider set makes this simpler than upserting. */
    void replaceAll(List<ProviderSetting> settings);
}
