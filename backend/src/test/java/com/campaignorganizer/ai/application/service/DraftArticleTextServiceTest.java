package com.campaignorganizer.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.ai.application.port.in.DraftArticleTextUseCase.DraftArticleTextCommand;
import com.campaignorganizer.ai.application.port.out.TextGenerationFailedException;
import com.campaignorganizer.ai.application.port.out.TextGenerationPort;
import com.campaignorganizer.ai.domain.AiUnavailableException;
import com.campaignorganizer.ai.domain.DraftResult;
import com.campaignorganizer.shared.domain.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Application service unit test with mocked out-ports - no Spring, no HTTP. Covers
 * the one piece of real logic this stateless context has: provider fallback order.
 */
@ExtendWith(MockitoExtension.class)
class DraftArticleTextServiceTest {

    @Mock
    private TextGenerationPort primary;
    @Mock
    private TextGenerationPort secondary;

    @Test
    void firstProviderSucceeding_isReturnedWithoutTryingTheSecond() {
        when(primary.generate(anyString(), anyString())).thenReturn(new DraftResult("drafted text", "groq"));
        var service = new DraftArticleTextService(List.of(primary, secondary));

        DraftResult result = service.draft(new DraftArticleTextCommand("a gruff dockmaster", ""));

        assertThat(result.text()).isEqualTo("drafted text");
        assertThat(result.provider()).isEqualTo("groq");
        verify(secondary, never()).generate(anyString(), anyString());
    }

    @Test
    void firstProviderFailing_fallsBackToSecond() {
        when(primary.generate(anyString(), anyString()))
                .thenThrow(new TextGenerationFailedException("rate limited"));
        when(secondary.generate(anyString(), anyString()))
                .thenReturn(new DraftResult("fallback text", "openrouter"));
        var service = new DraftArticleTextService(List.of(primary, secondary));

        DraftResult result = service.draft(new DraftArticleTextCommand("a gruff dockmaster", ""));

        assertThat(result.provider()).isEqualTo("openrouter");
    }

    @Test
    void everyProviderFailing_throwsAiUnavailable() {
        when(primary.generate(anyString(), anyString())).thenThrow(new TextGenerationFailedException("down"));
        when(secondary.generate(anyString(), anyString())).thenThrow(new TextGenerationFailedException("down"));
        var service = new DraftArticleTextService(List.of(primary, secondary));

        assertThatThrownBy(() -> service.draft(new DraftArticleTextCommand("a gruff dockmaster", "")))
                .isInstanceOf(AiUnavailableException.class);
    }

    @Test
    void blankInstructions_rejectedBeforeAnyProviderIsCalled() {
        var service = new DraftArticleTextService(List.of(primary, secondary));

        assertThatThrownBy(() -> service.draft(new DraftArticleTextCommand("  ", "")))
                .isInstanceOf(ValidationException.class);
        verify(primary, never()).generate(anyString(), anyString());
    }
}
