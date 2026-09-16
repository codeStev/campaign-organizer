package com.campaignorganizer.config.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Provisions the two non-superuser Postgres roles the app needs for row-level
 * security to actually apply (ADR-0114): {@code app_runtime} (the app's own
 * JPA/Hikari connection, {@code spring.datasource.*}) and
 * {@code app_rls_bypass} (a NOLOGIN role with BYPASSRLS, granted to
 * {@code app_runtime} as dormant membership only — {@code app_runtime} is
 * created NOINHERIT so this grant never applies silently; it's only assumed
 * via an explicit {@code SET LOCAL ROLE}, see
 * {@code FirstAccountOwnershipBootstrapper}).
 *
 * <p>Runs on every {@code afterMigrate} (every startup, not a one-time setup
 * script) using Flyway's own superuser ({@code app}) connection — so a table
 * added by the very migration that just ran is granted too, without this
 * class needing to change. Every statement is idempotent: safe to run against
 * both a brand-new database and one that's already provisioned.
 */
@Component
public class RuntimeRoleProvisioningCallback extends BaseCallback {

    private static final String RUNTIME_ROLE = "app_runtime";
    private static final String BYPASS_ROLE = "app_rls_bypass";

    private final String runtimePassword;

    public RuntimeRoleProvisioningCallback(@Value("${spring.datasource.password}") String runtimePassword) {
        this.runtimePassword = runtimePassword;
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            createRoleIfMissing(statement, RUNTIME_ROLE, "NOSUPERUSER NOINHERIT LOGIN");
            createRoleIfMissing(statement, BYPASS_ROLE, "NOSUPERUSER NOLOGIN BYPASSRLS");
            statement.execute(
                    "ALTER ROLE " + RUNTIME_ROLE + " WITH PASSWORD '" + escapeLiteral(runtimePassword) + "'");
            statement.execute("GRANT " + BYPASS_ROLE + " TO " + RUNTIME_ROLE);
            statement.execute("GRANT USAGE ON SCHEMA public TO " + RUNTIME_ROLE + ", " + BYPASS_ROLE);
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO "
                    + RUNTIME_ROLE + ", " + BYPASS_ROLE);
            statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE "
                    + "ON TABLES TO " + RUNTIME_ROLE + ", " + BYPASS_ROLE);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to provision RLS runtime roles (ADR-0114)", ex);
        }
    }

    private void createRoleIfMissing(Statement statement, String role, String attributes) throws SQLException {
        statement.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '%s') THEN
                    EXECUTE 'CREATE ROLE %s %s';
                  END IF;
                END
                $$;
                """.formatted(role, role, attributes));
    }

    /** Postgres string-literal escaping (double any single quote) — the password never contains SQL, only text. */
    private String escapeLiteral(String value) {
        return value.replace("'", "''");
    }

    @Override
    public String getCallbackName() {
        return "runtimeRoleProvisioning";
    }
}
