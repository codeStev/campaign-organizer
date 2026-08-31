package com.campaignorganizer.ai.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SummarizeSessionNotesRequest(@NotBlank @Size(max = 20000) String notes) {
}
