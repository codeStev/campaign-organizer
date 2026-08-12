package com.campaignorganizer.whiteboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class WhiteboardControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/worlds/{w}/whiteboards", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReadUpdateDeleteWithNodesAndEdges() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String body = "{\"name\":\"Plot\",\"nodes\":["
                + "{\"id\":\"n1\",\"text\":\"Hook\",\"x\":10,\"y\":20,\"color\":\"#6d54c9\"},"
                + "{\"id\":\"n2\",\"text\":\"Twist\",\"x\":200,\"y\":80}],"
                + "\"edges\":[{\"id\":\"e1\",\"fromNodeId\":\"n1\",\"toNodeId\":\"n2\",\"label\":\"leads to\"}]}";

        String created = mockMvc.perform(post("/api/worlds/{w}/whiteboards", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plot"))
                .andExpect(jsonPath("$.nodes.length()").value(2))
                .andExpect(jsonPath("$.nodes[0].text").value("Hook"))
                .andExpect(jsonPath("$.nodes[1].x").value(200))
                .andExpect(jsonPath("$.edges[0].label").value("leads to"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/api/worlds/{w}/whiteboards/{b}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edges[0].fromNodeId").value("n1"));

        mockMvc.perform(put("/api/worlds/{w}/whiteboards/{b}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plot v2\",\"nodes\":[{\"id\":\"n1\",\"text\":\"Only\",\"x\":0,\"y\":0}],\"edges\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Plot v2"))
                .andExpect(jsonPath("$.nodes.length()").value(1))
                .andExpect(jsonPath("$.edges.length()").value(0));

        mockMvc.perform(delete("/api/worlds/{w}/whiteboards/{b}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsWhiteboardWithoutName() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        mockMvc.perform(post("/api/worlds/{w}/whiteboards", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nodes\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
