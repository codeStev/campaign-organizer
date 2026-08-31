package com.campaignorganizer.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.ai.application.port.in.DraftArticleTextUseCase.DraftArticleTextCommand;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.ai.domain.ArticleKind;
import com.campaignorganizer.ai.domain.DraftLevel;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.ai.domain.ProviderSetting;
import com.campaignorganizer.shared.domain.ValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Application service unit test with mocked out-ports - no Spring, no HTTP. Covers
 * the one piece of real logic this stateless context has: provider fallback order,
 * driven by persisted settings (ADR-0065) rather than static @Order.
 */
@ExtendWith(MockitoExtension.class)
class DraftArticleTextServiceTest {

    @Mock
    private TextGenerationPort groq;
    @Mock
    private TextGenerationPort openRouter;
    @Mock
    private AiProviderSettingsRepositoryPort settingsRepository;

    private DraftArticleTextService service;

    @BeforeEach
    void setUp() {
        lenient().when(groq.providerId()).thenReturn("groq");
        lenient().when(groq.defaultModel()).thenReturn("groq-default");
        lenient().when(groq.configured()).thenReturn(true);
        lenient().when(openRouter.providerId()).thenReturn("openrouter");
        lenient().when(openRouter.defaultModel()).thenReturn("openrouter-default");
        lenient().when(openRouter.configured()).thenReturn(true);
        // Empty settings -> DefaultProviderSettings.orDefaults() applies: groq then openrouter.
        lenient().when(settingsRepository.findAllOrderedByPriority()).thenReturn(List.of());
        service = new DraftArticleTextService(
                new ProviderFallbackTextGenerator(List.of(groq, openRouter), settingsRepository));
    }

    @Test
    void firstProviderSucceeding_isReturnedWithoutTryingTheSecond() {
        when(groq.generate(anyString(), anyString(), anyString()))
                .thenReturn(new DraftResult("drafted text", "groq"));

        DraftResult result = service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC));

        assertThat(result.text()).isEqualTo("drafted text");
        assertThat(result.provider()).isEqualTo("groq");
        verify(openRouter, never()).generate(anyString(), anyString(), anyString());
    }

    @Test
    void firstProviderFailing_fallsBackToSecond() {
        when(groq.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("rate limited"));
        when(openRouter.generate(anyString(), anyString(), anyString()))
                .thenReturn(new DraftResult("fallback text", "openrouter"));

        DraftResult result = service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC));

        assertThat(result.provider()).isEqualTo("openrouter");
    }

    @Test
    void everyProviderFailing_throwsAiUnavailable() {
        when(groq.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("down"));
        when(openRouter.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("down"));

        assertThatThrownBy(() -> service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC)))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void blankInstructions_rejectedBeforeAnyProviderIsCalled() {
        assertThatThrownBy(() -> service.draft(
                new DraftArticleTextCommand("  ", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC)))
                .isInstanceOf(ValidationException.class);
        verify(groq, never()).generate(anyString(), anyString(), anyString());
    }

    @Test
    void nullLevelAndKind_defaultToFullDraftAndGeneric() {
        when(groq.generate(anyString(), anyString(), anyString()))
                .thenReturn(new DraftResult("drafted text", "groq"));

        service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", null, null));
        service.draft(new DraftArticleTextCommand(
                "a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC));

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(groq, times(2)).generate(systemPrompt.capture(), anyString(), anyString());
        assertThat(systemPrompt.getAllValues().get(0)).isEqualTo(systemPrompt.getAllValues().get(1));
    }

    @Test
    void quickInspirationLevel_usesTheTeaserPrompt() {
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        when(groq.generate(systemPrompt.capture(), anyString(), anyString()))
                .thenReturn(new DraftResult("a teaser", "groq"));

        service.draft(new DraftArticleTextCommand(
                "a gruff dockmaster", "", DraftLevel.QUICK_INSPIRATION, ArticleKind.GENERIC));

        assertThat(systemPrompt.getValue()).contains("a spark, not a finished article");
    }

    @Test
    void readAloudLevel_usesTheBoxedTextPrompt() {
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        when(groq.generate(systemPrompt.capture(), anyString(), anyString()))
                .thenReturn(new DraftResult("you see...", "groq"));

        service.draft(new DraftArticleTextCommand(
                "a gruff dockmaster", "", DraftLevel.READ_ALOUD, ArticleKind.GENERIC));

        assertThat(systemPrompt.getValue()).contains("read-aloud/boxed text");
    }

    @Test
    void basicInfoLevel_forbidsInventingNewEntities() {
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        when(groq.generate(systemPrompt.capture(), anyString(), anyString()))
                .thenReturn(new DraftResult("the basics", "groq"));

        service.draft(new DraftArticleTextCommand(
                "a gruff dockmaster", "", DraftLevel.BASIC_INFO, ArticleKind.GENERIC));

        assertThat(systemPrompt.getValue()).contains("Do not invent or name any new places");
    }

    @Test
    void characterKind_addsCharacterFocusedGuidance() {
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        when(groq.generate(systemPrompt.capture(), anyString(), anyString()))
                .thenReturn(new DraftResult("a character", "groq"));

        service.draft(new DraftArticleTextCommand(
                "a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.CHARACTER));

        assertThat(systemPrompt.getValue())
                .contains("This article is about a character")
                .contains("personality, appearance, motivations");
    }

    @Test
    void savedPrioritySwapsTryOrder() {
        when(settingsRepository.findAllOrderedByPriority()).thenReturn(List.of(
                new ProviderSetting("openrouter", "custom-model", 0),
                new ProviderSetting("groq", null, 1)));
        when(openRouter.generate(anyString(), anyString(), eq("custom-model")))
                .thenReturn(new DraftResult("from openrouter first", "openrouter"));

        DraftResult result = service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC));

        assertThat(result.provider()).isEqualTo("openrouter");
        verify(openRouter).generate(anyString(), anyString(), eq("custom-model"));
        verify(groq, never()).generate(anyString(), anyString(), anyString());
    }

    @Test
    void unconfiguredProvider_isSkippedWithoutAnAttemptedCall() {
        when(groq.configured()).thenReturn(false); // no GROQ_API_KEY in this scenario
        when(openRouter.generate(anyString(), anyString(), anyString()))
                .thenReturn(new DraftResult("fallback text", "openrouter"));

        DraftResult result = service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC));

        assertThat(result.provider()).isEqualTo("openrouter");
        verify(groq, never()).generate(anyString(), anyString(), anyString());
    }

    @Test
    void everyConfiguredProviderFailing_whileOneLacksAKey_stillThrowsAiUnavailable() {
        when(groq.configured()).thenReturn(false);
        when(openRouter.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("503 upstream"));

        assertThatThrownBy(() -> service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC)))
                .isInstanceOf(AiUnavailableException.class);
        verify(groq, never()).generate(anyString(), anyString(), anyString());
    }

    @Test
    void noProvidersConfiguredAtAll_throwsAiUnavailableWithoutAnyCall() {
        when(groq.configured()).thenReturn(false);
        when(openRouter.configured()).thenReturn(false);

        assertThatThrownBy(() -> service.draft(new DraftArticleTextCommand("a gruff dockmaster", "", DraftLevel.FULL_DRAFT, ArticleKind.GENERIC)))
                .isInstanceOf(AiUnavailableException.class);
        verify(groq, never()).generate(anyString(), anyString(), anyString());
        verify(openRouter, never()).generate(anyString(), anyString(), anyString());
    }
}
