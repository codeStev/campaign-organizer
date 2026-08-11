package com.campaignorganizer.media;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class MediaControllerIT extends AbstractIntegrationTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4};

    @Test
    void uploadsListsServesAndDeletes() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        var file = new MockMultipartFile("file", "map.png", "image/png", PNG);
        String created = mockMvc.perform(multipart("/api/worlds/{w}/media", worldId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("map.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.size").value(PNG.length))
                .andExpect(jsonPath("$.url").exists())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(created, "$.id");
        String url = JsonPath.read(created, "$.url");

        // Listed
        mockMvc.perform(get("/api/worlds/{w}/media", worldId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Served publicly (no auth header) with the original bytes.
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        // Deleted
        mockMvc.perform(delete("/api/worlds/{w}/media/{m}", worldId, id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsNonImageUpload() throws Exception {
        String auth = authHeader();
        String worldId = createWorld(auth);

        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/worlds/{w}/media", worldId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        var file = new MockMultipartFile("file", "map.png", "image/png", PNG);
        mockMvc.perform(multipart("/api/worlds/{w}/media", java.util.UUID.randomUUID()).file(file))
                .andExpect(status().isUnauthorized());
    }
}
