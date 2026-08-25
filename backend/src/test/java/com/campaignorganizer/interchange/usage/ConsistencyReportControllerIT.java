package com.campaignorganizer.interchange.usage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * The report end-to-end: real persistence, real wiki resolution. Seeds a
 * known graph — two linked articles plus a forgotten one, a campaign whose
 * beat references one of them, and a roll table linking a missing article —
 * then checks every section against it.
 */
class ConsistencyReportControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/consistency-report", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownWorld_is404() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/consistency-report", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void report_coversBrokenLinksOrphansAndCampaignGaps() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String lighthouse = createArticle(auth, worldId, "Lighthouse", "A tall tower.");
        createArticle(auth, worldId, "Harbor",
                "See [[lighthouse]] and [[ghost ship]].");
        createArticle(auth, worldId, "Forgotten", "Nobody mentions me.");

        String campaignId = createCampaign(auth, worldId);
        String arcId = createArc(auth, worldId, campaignId);
        createBeat(auth, worldId, campaignId, arcId, "Dock fire", "[[harbor]] burns",
                "[\"" + lighthouse + "\"]");

        postJson(auth, "/api/worlds/{w}/roll-tables", worldId, """
                {"title":"Weather","description":null,"diceExpression":"1d6",
                 "entries":[{"minResult":null,"maxResult":null,
                             "body":"[[Ghost Ship]] sighted"}]}
                """);

        mockMvc.perform(get("/api/worlds/{w}/consistency-report", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                // [[ghost ship]] is unresolvable from both article and table prose.
                .andExpect(jsonPath("$.brokenLinks[*].target",
                        Matchers.hasItems("ghost ship")))
                .andExpect(jsonPath("$.brokenLinks[?(@.sourceType=='ARTICLE')].sourceLabel",
                        Matchers.contains("Harbor")))
                .andExpect(jsonPath("$.brokenLinks[?(@.sourceType=='ROLL_TABLE')].sourceLabel",
                        Matchers.hasItem(Matchers.containsString("Weather"))))
                // Only "Forgotten" has no inbound reference of any kind.
                .andExpect(jsonPath("$.orphanedArticles[*].title", Matchers.contains("Forgotten")))
                // The beat pulls only the Lighthouse into the campaign.
                .andExpect(jsonPath("$.unreferencedByCampaigns[*].title",
                        Matchers.containsInAnyOrder("Harbor", "Forgotten")));
    }

    private String createArticle(String auth, String worldId, String title, String body)
            throws Exception {
        String json = "{\"title\":\"" + title + "\",\"categoryId\":null,\"body\":\"" + body + "\"}";
        String response = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createCampaign(String auth, String worldId) throws Exception {
        String response = mockMvc.perform(post("/api/worlds/{w}/campaigns", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Main\",\"description\":null}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createArc(String auth, String worldId, String campaignId) throws Exception {
        String response = mockMvc.perform(
                        post("/api/worlds/{w}/campaigns/{c}/arcs", worldId, campaignId)
                                .header(HttpHeaders.AUTHORIZATION, auth)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"title\":\"Act I\",\"description\":null}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void createBeat(String auth, String worldId, String campaignId, String arcId,
                            String title, String body, String articleIdsJson) throws Exception {
        String json = "{\"title\":\"" + title + "\",\"body\":\"" + body
                + "\",\"done\":true,\"articleIds\":" + articleIdsJson + "}";
        mockMvc.perform(post("/api/worlds/{w}/campaigns/{c}/arcs/{a}/beats",
                                worldId, campaignId, arcId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is2xxSuccessful());
    }

    /** POST JSON meant to succeed; surfaces seeding errors instead of hiding them. */
    private void postJson(String auth, String path, Object... pathArgsAndBody) throws Exception {
        Object[] args = new Object[pathArgsAndBody.length - 1];
        System.arraycopy(pathArgsAndBody, 0, args, 0, args.length);
        String json = (String) pathArgsAndBody[pathArgsAndBody.length - 1];
        mockMvc.perform(post(path, args)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is2xxSuccessful());
    }
}
