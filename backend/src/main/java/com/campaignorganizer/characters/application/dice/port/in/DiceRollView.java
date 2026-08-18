package com.campaignorganizer.characters.application.dice.port.in;

import com.campaignorganizer.characters.domain.dice.DieRoll;
import java.util.List;

/** Application read model for a dice roll. */
public record DiceRollView(String expression, int total, List<DieRoll> rolls, String breakdown) {
}
