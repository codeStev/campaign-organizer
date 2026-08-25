package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.in.GetAiSettingsUseCase;
import com.campaignorganizer.ai.application.port.in.ProviderSettingView;
import com.campaignorganizer.ai.application.port.in.UpdateAiSettingsUseCase;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads/writes AI provider settings (ADR-0065). */
@Service
public class AiSettingsService implements GetAiSettingsUseCase, UpdateAiSettingsUseCase {

    private final AiProviderSettingsRepositoryPort repository;
    private final List<TextGenerationPort> providers;

    public AiSettingsService(AiProviderSettingsRepositoryPort repository, List<TextGenerationPort> providers) {
        this.repository = repository;
        this.providers = providers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderSettingView> get() {
        return toViews(DefaultProviderSettings.orDefaults(repository.findAllOrderedByPriority()));
    }

    @Override
    @Transactional
    public List<ProviderSettingView> update(UpdateAiSettingsCommand command) {
        List<ProviderSetting> settings = new ArrayList<>();
        int priority = 0;
        for (var input : command.providers()) {
            settings.add(new ProviderSetting(input.providerId(), input.model(), priority++));
        }
        repository.replaceAll(settings);
        return toViews(settings);
    }

    private List<ProviderSettingView> toViews(List<ProviderSetting> settings) {
        return settings.stream().map(setting -> {
            // Resolved per call: a constructor must not invoke methods on its ports.
            TextGenerationPort provider = providers.stream()
                    .filter(p -> setting.providerId().equals(p.providerId()))
                    .findAny()
                    .orElse(null);
            String defaultModel = provider == null ? null : provider.defaultModel();
            boolean configured = provider != null && provider.configured();
            return new ProviderSettingView(
                    setting.providerId(), setting.model(), defaultModel, configured, setting.priority());
        }).toList();
    }
}
