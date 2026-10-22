package com.tt.labexam.controller;

import com.tt.labexam.dto.DbHealthResponse;
import com.tt.labexam.dto.HealthResponse;
import com.tt.labexam.service.DatabaseHealthService;
import com.tt.labexam.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @MockitoBean
    private DatabaseHealthService databaseHealthService;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        when(healthService.getHealth())
                .thenReturn(new HealthResponse("UP", "lab-exam-backend", "17", "Spring Boot backend is running", 1L));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void databaseEndpointReportsConnected() throws Exception {
        when(databaseHealthService.check())
                .thenReturn(new DbHealthResponse(true, "MySQL", "8.0.36", "Database connection successful", 1L));

        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.connected").value(true));
    }

    @Test
    void databaseEndpointReportsFailureWithoutLeakingDetails() throws Exception {
        when(databaseHealthService.check())
                .thenReturn(new DbHealthResponse(false, "MySQL", null, "Database connection failed.", 1L));

        mockMvc.perform(get("/api/health/db"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connected").value(false))
                .andExpect(jsonPath("$.data.message").value("Database connection failed."));
    }
}
