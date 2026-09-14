package com.campaignorganizer.ai.application.service;

import com.campaignorganizer.ai.application.port.in.GetAiSettingsUseCase;
import com.campaignorganizer.ai.application.port.in.ProviderSettingView;
import com.campaignorganizer.ai.application.port.in.UpdateAiSettingsUseCase;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.application.port.published.AiSettingsOwnershipPort;
import com.campaignorganizer.ai.domain.ProviderSetting;
import com.campaignorganizer.security.CurrentUserPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads/writes AI provider settings (ADR-0065), scoped per account (ADR-0109). */
@Service
public class AiSettingsService implements GetAiSettingsUseCase, UpdateAiSettingsUseCase, AiSettingsOwnershipPort {

    private final AiProviderSettingsRepositoryPort repository;
    private final List<TextGenerationPort> providers;
    private final CurrentUserPort currentUser;

    public AiSettingsService(AiProviderSettingsRepositoryPort repository, List<TextGenerationPort> providers,
                             CurrentUserPort currentUser) {
        this.repository = repository;
        this.providers = providers;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProviderSettingView> get() {
        return toViews(DefaultProviderSettings.orDefaults(
                repository.findAllOrderedByPriority(currentUser.currentAccountId())));
    }

    @Override
    @Transactional
    public List<ProviderSettingView> update(UpdateAiSettingsCommand command) {
        List<ProviderSetting> settings = new ArrayList<>();
        int priority = 0;
        for (var input : command.providers()) {
            settings.add(new ProviderSetting(input.providerId(), input.model(), priority++));
        }
        UUID ownerId = currentUser.currentAccountId();
        repository.replaceAll(ownerId, settings);
        // Return the persisted state (includes any defaults applied by the repository)
        return toViews(repository.findAllOrderedByPriority(ownerId));
    }

    @Override
    @Transactional
    public void assignUnownedTo(UUID ownerId) {
        repository.assignUnownedTo(ownerId);
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
