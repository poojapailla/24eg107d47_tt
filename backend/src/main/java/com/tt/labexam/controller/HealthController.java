package com.tt.labexam.controller;

import com.tt.labexam.dto.ApiResponse;
import com.tt.labexam.dto.DbHealthResponse;
import com.tt.labexam.dto.HealthResponse;
import com.tt.labexam.service.DatabaseHealthService;
import com.tt.labexam.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;
    private final DatabaseHealthService databaseHealthService;

    public HealthController(HealthService healthService, DatabaseHealthService databaseHealthService) {
        this.healthService = healthService;
        this.databaseHealthService = databaseHealthService;
    }

    /** Simple liveness check for the backend itself. */
    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(healthService.getHealth(), "Backend is running");
    }

    /** Verifies that a real query can be executed against MySQL. */
    @GetMapping("/db")
    public ApiResponse<DbHealthResponse> database() {
        return ApiResponse.ok(databaseHealthService.check(), "Database check completed");
    }
}
