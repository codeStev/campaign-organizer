package com.campaignorganizer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for HTTP-level integration tests. Boots the full application
 * against one shared PostgreSQL container so Flyway migrations and JPA mapping
 * are exercised exactly as in production.
 *
 * <p>The container is a JVM-lifetime <b>singleton</b> started in a static
 * initializer (not via {@code @Testcontainers}/{@code @Container}). This keeps
 * it stable across the cached Spring context that all integration tests share;
 * a per-class container would be restarted underneath the cached context.
 *
 * <p>Payloads are built as plain JSON strings and responses inspected with
 * JsonPath to stay independent of which Jackson version Spring wires.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    /** Logs in with the test password and returns a ready {@code Bearer <jwt>} header value. */
    protected String authHeader() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"test-password\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    /** Creates a world and returns its id. */
    protected String createWorld(String auth) throws Exception {
        String body = mockMvc.perform(post("/api/worlds")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test World\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Uploads a tiny PNG into the world and returns the media id. */
    protected String uploadImage(String auth, String worldId) throws Exception {
        var file = new MockMultipartFile("file", "map.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4});
        String body = mockMvc.perform(multipart("/api/worlds/{w}/media", worldId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
