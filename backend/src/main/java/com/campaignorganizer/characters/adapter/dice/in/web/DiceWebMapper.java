package com.campaignorganizer.characters.adapter.dice.in.web;

import com.campaignorganizer.characters.adapter.dice.in.web.DiceWebDtos.DiceRollResponse;
import com.campaignorganizer.characters.adapter.dice.in.web.DiceWebDtos.DieRollResponse;
import com.campaignorganizer.characters.application.dice.port.in.DiceRollView;
import com.campaignorganizer.characters.domain.dice.DieRoll;
import org.mapstruct.Mapper;

/** Maps the dice view to the web response (MapStruct). */
@Mapper(componentModel = "spring")
public interface DiceWebMapper {

    DiceRollResponse toResponse(DiceRollView view);

    DieRollResponse toDieResponse(DieRoll roll);
}
