package com.campaignorganizer.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.ai.application.port.in.ProviderSettingView;
import com.campaignorganizer.ai.application.port.in.UpdateAiSettingsUseCase.UpdateAiSettingsCommand;
import com.campaignorganizer.ai.application.port.in.UpdateAiSettingsUseCase.UpdateAiSettingsCommand.ProviderSettingInput;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.ProviderSetting;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiSettingsServiceTest {

    @Mock
    private AiProviderSettingsRepositoryPort repository;
    @Mock
    private TextGenerationPort groq;
    @Mock
    private TextGenerationPort openRouter;

    private AiSettingsService service;

    @BeforeEach
    void setUp() {
        lenient().when(groq.providerId()).thenReturn("groq");
        lenient().when(groq.defaultModel()).thenReturn("groq-default");
        lenient().when(openRouter.providerId()).thenReturn("openrouter");
        lenient().when(openRouter.defaultModel()).thenReturn("openrouter-default");
        service = new AiSettingsService(repository, List.of(groq, openRouter));
    }

    @Test
    void getWithNoSavedSettings_returnsBuiltInDefaultsInOrder() {
        when(repository.findAllOrderedByPriority()).thenReturn(List.of());

        List<ProviderSettingView> views = service.get();

        assertThat(views).extracting(ProviderSettingView::providerId).containsExactly("groq", "openrouter");
        assertThat(views).extracting(ProviderSettingView::model).containsExactly(null, null);
    }

    @Test
    void getWithSavedSettings_reflectsPersistedModelAndConfiguredFlag() {
        when(groq.configured()).thenReturn(true);
        when(openRouter.configured()).thenReturn(false);
        when(repository.findAllOrderedByPriority()).thenReturn(List.of(
                new ProviderSetting("openrouter", "custom-model", 0),
                new ProviderSetting("groq", null, 1)));

        List<ProviderSettingView> views = service.get();

        assertThat(views).hasSize(2);
        ProviderSettingView first = views.get(0);
        assertThat(first.providerId()).isEqualTo("openrouter");
        assertThat(first.model()).isEqualTo("custom-model");
        assertThat(first.defaultModel()).isEqualTo("openrouter-default");
        assertThat(first.configured()).isFalse();
        assertThat(first.priority()).isEqualTo(0);
    }

    @Test
    void update_replacesSettingsWithPriorityDerivedFromListOrder() {
        var command = new UpdateAiSettingsCommand(List.of(
                new ProviderSettingInput("openrouter", "custom-model"),
                new ProviderSettingInput("groq", null)));

        service.update(command);

        ArgumentCaptor<List<ProviderSetting>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).replaceAll(captor.capture());
        List<ProviderSetting> saved = captor.getValue();
        assertThat(saved).extracting(ProviderSetting::providerId).containsExactly("openrouter", "groq");
        assertThat(saved).extracting(ProviderSetting::priority).containsExactly(0, 1);
    }

    @Test
    void update_returnsViewsReflectingTheNewPersistedSettings() {
        when(repository.findAllOrderedByPriority()).thenReturn(List.of(
                new ProviderSetting("groq", "custom-model", 0),
                new ProviderSetting("openrouter", null, 1)));
        var command = new UpdateAiSettingsCommand(List.of(new ProviderSettingInput("groq", "custom-model")));

        List<ProviderSettingView> views = service.update(command);

        assertThat(views).extracting(ProviderSettingView::providerId).containsExactly("groq", "openrouter");
        assertThat(views).extracting(ProviderSettingView::model).containsExactly("custom-model", null);
        verify(repository).replaceAll(anyList());
    }
}
