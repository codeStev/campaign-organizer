package com.campaignorganizer.ai.application.port.in;

import java.util.List;

public interface UpdateAiSettingsUseCase {

    /** Replaces all settings; list order becomes priority order. */
    List<ProviderSettingView> update(UpdateAiSettingsCommand command);

    record UpdateAiSettingsCommand(List<ProviderSettingInput> providers) {

        public record ProviderSettingInput(String providerId, String model) {
        }
    }
}
