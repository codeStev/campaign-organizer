package com.campaignorganizer.dice;

import com.campaignorganizer.dice.DiceRoller.DieRoll;
import com.campaignorganizer.dice.DiceRoller.RollResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Stateless dice roller. Not world-scoped — a utility for the table. */
@RestController
@RequestMapping("/api/dice")
public class DiceController {

    public record DiceRollRequest(@NotBlank @Size(max = 200) String expression) {
    }

    public record DieRollResponse(int sides, int value, boolean kept) {
        static DieRollResponse from(DieRoll r) {
            return new DieRollResponse(r.sides(), r.value(), r.kept());
        }
    }

    public record DiceRollResponse(String expression, int total, List<DieRollResponse> rolls,
                                   String breakdown) {
    }

    @PostMapping("/roll")
    public DiceRollResponse roll(@jakarta.validation.Valid @RequestBody DiceRollRequest request) {
        try {
            RollResult result = DiceRoller.roll(request.expression());
            return new DiceRollResponse(result.expression(), result.total(),
                    result.rolls().stream().map(DieRollResponse::from).toList(), result.breakdown());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}
