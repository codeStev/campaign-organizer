package com.campaignorganizer.ai.application.port.in;

import com.campaignorganizer.ai.domain.DraftResult;

public interface SummarizeSessionNotesUseCase {

    DraftResult summarize(SummarizeSessionNotesCommand command);

    record SummarizeSessionNotesCommand(String notes) {
    }
}
