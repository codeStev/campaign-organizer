package com.campaignorganizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * ADR-0114's "proven, not just argued" claim: connects directly as
 * {@code app_runtime} (bypassing every bit of application code — no
 * {@code WorldPermissionEvaluator}, no controller, nothing) and confirms RLS
 * itself, at the database layer, genuinely hides another account's world.
 */
class RowLevelSecurityIsolationIT extends AbstractIntegrationTest {

    @Test
    void anotherAccountsWorldIsInvisibleWithoutItsOwnerIdSetAsTheGuc() throws Exception {
        String authA = authHeader();
        UUID accountAId = currentAccountId(authA);
        String worldId = createWorld(authA);

        try (Connection connection = openAppRuntimeConnection()) {
            connection.setAutoCommit(false);

            setGuc(connection, UUID.randomUUID()); // some other account entirely
            assertThat(worldVisible(connection, worldId))
                    .as("world must be invisible under an unrelated account's GUC")
                    .isFalse();
            connection.rollback();

            connection.setAutoCommit(false);
            setGuc(connection, accountAId);
            assertThat(worldVisible(connection, worldId))
                    .as("world must be visible under its own owner's GUC")
                    .isTrue();
            connection.rollback();
        }
    }

    private UUID currentAccountId(String auth) throws Exception {
        String response = mockMvc.perform(get("/api/accounts/me").header(HttpHeaders.AUTHORIZATION, auth))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    private Connection openAppRuntimeConnection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), "app_runtime", RUNTIME_DB_PASSWORD);
    }

    private void setGuc(Connection connection, UUID accountId) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL app.current_account_id = '" + accountId + "'");
        }
    }

    private boolean worldVisible(Connection connection, String worldId) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT 1 FROM worlds WHERE id = '" + worldId + "'")) {
            return resultSet.next();
        }
    }
}
