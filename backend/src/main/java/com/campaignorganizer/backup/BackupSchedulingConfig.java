package com.campaignorganizer.backup;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on Spring's scheduler for {@link ScheduledBackups} (FR-42). */
@Configuration
@EnableScheduling
public class BackupSchedulingConfig {
}
