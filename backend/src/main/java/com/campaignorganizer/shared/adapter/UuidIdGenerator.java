package com.campaignorganizer.shared.adapter;

import com.campaignorganizer.shared.application.IdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Random UUID adapter for {@link IdGenerator}. */
@Component
public class UuidIdGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
