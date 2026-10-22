package com.tt.labexam.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RootController.class)
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootReturnsServiceLinks() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.health").value("/api/health"))
                .andExpect(jsonPath("$.data.database").value("/api/health/db"));
    }

    @Test
    void unknownPathReturnsJson404() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
