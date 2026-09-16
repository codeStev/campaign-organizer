package com.campaignorganizer.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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
            assertThat(rowVisible(connection, "worlds", worldId))
                    .as("world must be invisible under an unrelated account's GUC")
                    .isFalse();
            connection.rollback();

            connection.setAutoCommit(false);
            setGuc(connection, accountAId);
            assertThat(rowVisible(connection, "worlds", worldId))
                    .as("world must be visible under its own owner's GUC")
                    .isTrue();
            connection.rollback();
        }
    }

    @Test
    void anotherAccountsArticleIsInvisibleWithoutItsOwnerIdSetAsTheGuc() throws Exception {
        String authA = authHeader();
        UUID accountAId = currentAccountId(authA);
        String worldId = createWorld(authA);
        String articleId = createArticle(authA, worldId);

        try (Connection connection = openAppRuntimeConnection()) {
            connection.setAutoCommit(false);

            setGuc(connection, UUID.randomUUID());
            assertThat(rowVisible(connection, "articles", articleId))
                    .as("Tier 2 (articles, delegates to worlds): invisible under an unrelated account's GUC")
                    .isFalse();
            connection.rollback();

            connection.setAutoCommit(false);
            setGuc(connection, accountAId);
            assertThat(rowVisible(connection, "articles", articleId))
                    .as("Tier 2 (articles, delegates to worlds): visible under its own owner's GUC")
                    .isTrue();
            connection.rollback();
        }
    }

    @Test
    void anotherAccountsSessionIsInvisibleWithoutItsOwnerIdSetAsTheGuc() throws Exception {
        String authA = authHeader();
        UUID accountAId = currentAccountId(authA);
        String worldId = createWorld(authA);
        String campaignId = createCampaign(authA, worldId);
        String sessionId = createSession(authA, worldId, campaignId);

        try (Connection connection = openAppRuntimeConnection()) {
            connection.setAutoCommit(false);

            setGuc(connection, UUID.randomUUID());
            assertThat(rowVisible(connection, "sessions", sessionId))
                    .as("Tier 3 (sessions, delegates to campaigns): invisible under an unrelated account's GUC")
                    .isFalse();
            connection.rollback();

            connection.setAutoCommit(false);
            setGuc(connection, accountAId);
            assertThat(rowVisible(connection, "sessions", sessionId))
                    .as("Tier 3 (sessions, delegates to campaigns): visible under its own owner's GUC")
                    .isTrue();
            connection.rollback();
        }
    }

    @Test
    void anotherAccountsArcBeatIsInvisibleThroughTheFullDelegationChain() throws Exception {
        String authA = authHeader();
        UUID accountAId = currentAccountId(authA);
        String worldId = createWorld(authA);
        String campaignId = createCampaign(authA, worldId);
        String arcId = createArc(authA, worldId, campaignId);
        String beatId = createBeat(authA, worldId, campaignId, arcId);

        try (Connection connection = openAppRuntimeConnection()) {
            connection.setAutoCommit(false);

            setGuc(connection, UUID.randomUUID());
            assertThat(rowVisible(connection, "arc_beats", beatId))
                    .as("Tier 3 (arc_beats -> arcs -> campaigns -> worlds, 3-hop delegation): "
                            + "invisible under an unrelated account's GUC")
                    .isFalse();
            connection.rollback();

            connection.setAutoCommit(false);
            setGuc(connection, accountAId);
            assertThat(rowVisible(connection, "arc_beats", beatId))
                    .as("Tier 3 (arc_beats -> arcs -> campaigns -> worlds, 3-hop delegation): "
                            + "visible under its own owner's GUC")
                    .isTrue();
            connection.rollback();
        }
    }

    private String createCampaign(String auth, String worldId) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Campaign\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createSession(String auth, String worldId, String campaignId) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/sessions", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Session\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createArc(String auth, String worldId, String campaignId) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, campaignId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Arc\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createBeat(String auth, String worldId, String campaignId, String arcId) throws Exception {
        String response = mockMvc.perform(
                        post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats", worldId, campaignId, arcId)
                                .header(HttpHeaders.AUTHORIZATION, auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"Test Beat\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createArticle(String auth, String worldId) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Article\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
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

    private boolean rowVisible(Connection connection, String table, String id) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT 1 FROM " + table + " WHERE id = '" + id + "'")) {
            return resultSet.next();
        }
    }
}
