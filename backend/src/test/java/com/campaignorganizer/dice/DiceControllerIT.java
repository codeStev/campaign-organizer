package com.campaignorganizer.dice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campaignorganizer.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class DiceControllerIT extends AbstractIntegrationTest {

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/dice/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"1d6\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rollsWithinExpectedBounds() throws Exception {
        mockMvc.perform(post("/api/dice/roll")
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"2d6+3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolls.length()").value(2))
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.breakdown").isNotEmpty());
    }

    @Test
    void invalidExpressionIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/dice/roll")
                        .header(HttpHeaders.AUTHORIZATION, authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"nonsense\"}"))
                .andExpect(status().isBadRequest());
    }
}
