package com.tt.labexam.service;

import com.tt.labexam.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse(
                "UP",
                "lab-exam-backend",
                System.getProperty("java.version", "unknown"),
                "Spring Boot backend is running",
                System.currentTimeMillis());
    }
}
