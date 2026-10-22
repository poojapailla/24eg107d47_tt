package com.tt.labexam.controller;

import com.tt.labexam.dto.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Friendly landing endpoint so opening the Render URL does not return a 404. */
@RestController
public class RootController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        return ApiResponse.ok(
                Map.of(
                        "service", "lab-exam-backend",
                        "health", "/api/health",
                        "database", "/api/health/db"),
                "Backend is running. Use /api/health and /api/health/db.");
    }
}
