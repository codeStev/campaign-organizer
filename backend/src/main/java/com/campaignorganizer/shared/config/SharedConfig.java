package com.campaignorganizer.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires shared kernel beans (e.g. the {@link Clock} the application layer injects). */
@Configuration
public class SharedConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
