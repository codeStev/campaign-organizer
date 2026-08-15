package com.campaignorganizer.characters.adapter.dice.in.web;

import com.campaignorganizer.characters.adapter.dice.in.web.DiceWebDtos.DiceRollRequest;
import com.campaignorganizer.characters.adapter.dice.in.web.DiceWebDtos.DiceRollResponse;
import com.campaignorganizer.characters.application.dice.port.in.RollDiceUseCase;
import com.campaignorganizer.characters.application.dice.port.in.RollDiceUseCase.RollDiceCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for the stateless dice roller (not world-scoped). */
@RestController
@RequestMapping("/api/dice")
public class DiceController {

    private final RollDiceUseCase rollUseCase;
    private final DiceWebMapper mapper;

    public DiceController(RollDiceUseCase rollUseCase, DiceWebMapper mapper) {
        this.rollUseCase = rollUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/roll")
    public DiceRollResponse roll(@Valid @RequestBody DiceRollRequest request) {
        return mapper.toResponse(rollUseCase.roll(new RollDiceCommand(request.expression())));
    }
}
