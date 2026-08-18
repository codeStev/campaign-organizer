package com.campaignorganizer.characters.adapter.dice.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Web request/response models for dice rolling. */
public final class DiceWebDtos {

    private DiceWebDtos() {
    }

    public record DiceRollRequest(@NotBlank @Size(max = 200) String expression) {
    }

    public record DieRollResponse(int sides, int value, boolean kept) {
    }

    public record DiceRollResponse(String expression, int total, List<DieRollResponse> rolls,
                                   String breakdown) {
    }
}
