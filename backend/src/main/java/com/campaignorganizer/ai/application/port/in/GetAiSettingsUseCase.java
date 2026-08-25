package com.campaignorganizer.ai.application.port.in;

import java.util.List;

public interface GetAiSettingsUseCase {

    /** Persisted settings if any exist, otherwise the built-in defaults (ADR-0065). */
    List<ProviderSettingView> get();
}
