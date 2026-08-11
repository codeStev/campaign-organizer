package com.campaignorganizer.wiki;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class CategoryControllerIT extends AbstractIntegrationTest {

    @Test
    void createsNestedCategoriesAndDeletes() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String root = mockMvc.perform(post("/api/worlds/{w}/categories", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Geography\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String rootId = JsonPath.read(root, "$.id");

        mockMvc.perform(post("/api/worlds/{w}/categories", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cities\",\"parentId\":\"" + rootId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(rootId));

        mockMvc.perform(get("/api/worlds/{w}/categories", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/api/worlds/{w}/categories/{c}", worldId, rootId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void updatesCategoryAndRejectsSelfParent() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);
        String cat = mockMvc.perform(post("/api/worlds/{w}/categories", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Places\"}"))
                .andReturn().getResponse().getContentAsString();
        String catId = JsonPath.read(cat, "$.id");

        mockMvc.perform(put("/api/worlds/{w}/categories/{c}", worldId, catId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Locations\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Locations"));

        // A category cannot be its own parent.
        mockMvc.perform(put("/api/worlds/{w}/categories/{c}", worldId, catId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Locations\",\"parentId\":\"" + catId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsParentFromAnotherWorld() throws Exception {
        String auth = authHeader();
        String worldA = createWorld(auth);
        String worldB = createWorld(auth);

        String foreign = mockMvc.perform(post("/api/worlds/{w}/categories", worldB)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Foreign\"}"))
                .andReturn().getResponse().getContentAsString();
        String foreignId = JsonPath.read(foreign, "$.id");

        mockMvc.perform(post("/api/worlds/{w}/categories", worldA)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Child\",\"parentId\":\"" + foreignId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void articleKeepsSurvivingWhenCategoryDeleted() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        String cat = mockMvc.perform(post("/api/worlds/{w}/categories", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bestiary\"}"))
                .andReturn().getResponse().getContentAsString();
        String catId = JsonPath.read(cat, "$.id");

        String art = mockMvc.perform(post("/api/worlds/{w}/articles", worldId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dragon\",\"categoryId\":\"" + catId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(catId))
                .andReturn().getResponse().getContentAsString();
        String artId = JsonPath.read(art, "$.id");

        mockMvc.perform(delete("/api/worlds/{w}/categories/{c}", worldId, catId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        // Article survives, now uncategorized.
        mockMvc.perform(get("/api/worlds/{w}/articles/{a}", worldId, artId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").doesNotExist());
    }
}
