package com.campaignorganizer.characters.adapter.dice.out.random;

import com.campaignorganizer.characters.domain.dice.RandomSource;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/** Production randomness for dice rolls. */
@Component
public class ThreadLocalRandomSource implements RandomSource {

    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}
