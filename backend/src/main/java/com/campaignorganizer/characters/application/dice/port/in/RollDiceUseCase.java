package com.campaignorganizer.characters.application.dice.port.in;

public interface RollDiceUseCase {

    DiceRollView roll(RollDiceCommand command);

    record RollDiceCommand(String expression) {
    }
}
