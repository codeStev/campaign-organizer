package com.campaignorganizer.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.ai.application.port.in.TestAiProviderUseCase.ProviderTestView;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.ai.domain.ProviderSetting;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** The Settings "Test" button's logic: targeted, never falls back, reports failures as data. */
@ExtendWith(MockitoExtension.class)
class TestAiProviderServiceTest {

    @Mock
    private TextGenerationPort groq;
    @Mock
    private TextGenerationPort openRouter;
    @Mock
    private AiProviderSettingsRepositoryPort settingsRepository;

    private TestAiProviderService service;

    @BeforeEach
    void setUp() {
        lenient().when(groq.providerId()).thenReturn("groq");
        lenient().when(groq.defaultModel()).thenReturn("groq-default");
        lenient().when(openRouter.providerId()).thenReturn("openrouter");
        lenient().when(openRouter.defaultModel()).thenReturn("openrouter-default");
        // Saved settings: openrouter first with a custom model.
        lenient().when(settingsRepository.findAllOrderedByPriority()).thenReturn(List.of(
                new ProviderSetting("openrouter", "custom-model", 0),
                new ProviderSetting("groq", null, 1)));
        service = new TestAiProviderService(List.of(groq, openRouter), settingsRepository);
    }

    @Test
    void unknownProvider_isNotFound() {
        assertThatThrownBy(() -> service.test("nope"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unconfiguredProvider_reportsFailureAsDataWithoutACall() {
        when(groq.configured()).thenReturn(false);

        ProviderTestView view = service.test("groq");

        assertThat(view.ok()).isFalse();
        assertThat(view.error()).contains("API key");
        verify(groq, never()).generate(anyString(), anyString(), anyString());
    }

    @Test
    void success_usesPersistedModelAndReportsLatency() {
        when(openRouter.configured()).thenReturn(true);
        when(openRouter.generate(anyString(), anyString(), eq("custom-model")))
                .thenReturn(new DraftResult("OK", "openrouter"));

        ProviderTestView view = service.test("openrouter");

        assertThat(view.ok()).isTrue();
        assertThat(view.model()).isEqualTo("custom-model");
        assertThat(view.error()).isNull();
        assertThat(view.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void providerFailure_reportsOkFalseWithTheReason_notAnException() {
        when(openRouter.configured()).thenReturn(true);
        when(openRouter.generate(anyString(), anyString(), eq("custom-model")))
                .thenThrow(new TextGenerationFailedException("429 rate limited"));

        ProviderTestView view = service.test("openrouter");

        assertThat(view.ok()).isFalse();
        assertThat(view.error()).contains("429");
    }

    @Test
    void testingOneProvider_neverTouchesTheOthers() {
        when(groq.configured()).thenReturn(true);
        when(groq.generate(anyString(), anyString(), eq("groq-default")))
                .thenReturn(new DraftResult("OK", "groq"));

        service.test("groq");

        verify(openRouter, never()).generate(anyString(), anyString(), anyString());
    }
}
