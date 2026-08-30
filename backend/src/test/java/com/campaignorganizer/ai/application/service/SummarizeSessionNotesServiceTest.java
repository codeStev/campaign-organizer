package com.campaignorganizer.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.ai.application.port.in.SummarizeSessionNotesUseCase.SummarizeSessionNotesCommand;
import com.campaignorganizer.ai.application.port.out.AiProviderSettingsRepositoryPort;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.shared.domain.ValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Application service unit test with mocked out-ports - no Spring, no HTTP.
 * Provider fallback order itself is {@link ProviderFallbackTextGenerator}'s
 * responsibility and is covered exhaustively by
 * {@link DraftArticleTextServiceTest}; this test covers what's specific to
 * this use case: notes validation and the summarization prompt.
 */
@ExtendWith(MockitoExtension.class)
class SummarizeSessionNotesServiceTest {

    @Mock
    private TextGenerationPort groq;
    @Mock
    private AiProviderSettingsRepositoryPort settingsRepository;

    private SummarizeSessionNotesService service;

    @BeforeEach
    void setUp() {
        lenient().when(groq.providerId()).thenReturn("groq");
        lenient().when(groq.defaultModel()).thenReturn("groq-default");
        lenient().when(groq.configured()).thenReturn(true);
        lenient().when(settingsRepository.findAllOrderedByPriority()).thenReturn(List.of());
        service = new SummarizeSessionNotesService(
                new ProviderFallbackTextGenerator(List.of(groq), settingsRepository));
    }

    @Test
    void successfulSummary_returnsTextAndProvider() {
        when(groq.generate(anyString(), anyString(), anyString()))
                .thenReturn(new DraftResult("The party found the map.", "groq"));

        DraftResult result = service.summarize(new SummarizeSessionNotesCommand("we found a map in the crypt"));

        assertThat(result.text()).isEqualTo("The party found the map.");
        assertThat(result.provider()).isEqualTo("groq");
    }

    @Test
    void notesArePassedAsTheUserPrompt() {
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        when(groq.generate(anyString(), userPrompt.capture(), anyString()))
                .thenReturn(new DraftResult("summary", "groq"));

        service.summarize(new SummarizeSessionNotesCommand("we found a map in the crypt"));

        assertThat(userPrompt.getValue()).isEqualTo("we found a map in the crypt");
    }

    @Test
    void systemPrompt_carriesTheSystemAgnosticConstraint() {
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        when(groq.generate(systemPrompt.capture(), anyString(), anyString()))
                .thenReturn(new DraftResult("summary", "groq"));

        service.summarize(new SummarizeSessionNotesCommand("we found a map in the crypt"));

        assertThat(systemPrompt.getValue()).contains("system-agnostic");
    }

    @Test
    void blankNotes_rejectedBeforeAnyProviderIsCalled() {
        assertThatThrownBy(() -> service.summarize(new SummarizeSessionNotesCommand("   ")))
                .isInstanceOf(ValidationException.class);
        verify(groq, never()).generate(anyString(), anyString(), anyString());
    }

    @Test
    void everyProviderFailing_throwsAiUnavailable() {
        when(groq.generate(anyString(), anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("down"));

        assertThatThrownBy(() -> service.summarize(new SummarizeSessionNotesCommand("we found a map")))
                .isInstanceOf(AiUnavailableException.class);
    }
}
