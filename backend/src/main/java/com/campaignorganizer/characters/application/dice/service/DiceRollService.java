package com.campaignorganizer.characters.application.dice.service;

import com.campaignorganizer.characters.application.dice.port.in.DiceRollView;
import com.campaignorganizer.characters.application.dice.port.in.RollDiceUseCase;
import com.campaignorganizer.characters.domain.dice.DiceRoller;
import com.campaignorganizer.characters.domain.dice.RandomSource;
import com.campaignorganizer.characters.domain.dice.RollResult;
import org.springframework.stereotype.Service;

/** Dice-rolling use case; injects the randomness source and delegates to the domain. */
@Service
public class DiceRollService implements RollDiceUseCase {

    private final RandomSource random;

    public DiceRollService(RandomSource random) {
        this.random = random;
    }

    @Override
    public DiceRollView roll(RollDiceCommand command) {
        RollResult result = DiceRoller.roll(command.expression(), random);
        return new DiceRollView(result.expression(), result.total(), result.rolls(), result.breakdown());
    }
}
