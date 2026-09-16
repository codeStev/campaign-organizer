package com.campaignorganizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ADR-0114's schema-level fitness function: every Tier-1 table must have RLS
 * both enabled and forced, and at least one policy — the same "can't silently
 * regress" motivation as {@code ArchitectureTest.contextsOnlyUsePublishedPorts},
 * aimed at the schema instead of the package graph. Runs through the app's own
 * {@code app_runtime} connection (via the shared Testcontainers Postgres),
 * proving the fitness check itself works under the same role the real app uses.
 */
class RowLevelSecurityFitnessIT extends AbstractIntegrationTest {

    private static final List<String> TIER_1_TABLES = List.of(
            "worlds", "game_systems", "global_field_templates", "global_statblocks", "ai_provider_settings");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyTier1TableHasRlsEnabledAndForced() {
        for (String table : TIER_1_TABLES) {
            Boolean enabled = jdbcTemplate.queryForObject(
                    "SELECT relrowsecurity FROM pg_class WHERE relname = ? AND relnamespace = 'public'::regnamespace",
                    Boolean.class, table);
            Boolean forced = jdbcTemplate.queryForObject(
                    "SELECT relforcerowsecurity FROM pg_class WHERE relname = ? "
                            + "AND relnamespace = 'public'::regnamespace",
                    Boolean.class, table);
            assertThat(enabled).as("%s: ENABLE ROW LEVEL SECURITY", table).isTrue();
            assertThat(forced).as("%s: FORCE ROW LEVEL SECURITY", table).isTrue();
        }
    }

    @Test
    void everyTier1TableHasAtLeastOnePolicy() {
        for (String table : TIER_1_TABLES) {
            Integer policyCount = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_policies WHERE schemaname = 'public' AND tablename = ?",
                    Integer.class, table);
            assertThat(policyCount).as("%s: policy count", table).isGreaterThan(0);
        }
    }
}
