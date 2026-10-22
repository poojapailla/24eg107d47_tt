package com.tt.labexam.dto;

/** Payload of GET /api/health */
public record HealthResponse(String status, String service, String javaVersion, String message, long timestamp) {
}
