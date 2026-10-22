package com.tt.labexam.dto;

/** Payload of GET /api/health/db - never contains connection strings or credentials. */
public record DbHealthResponse(boolean connected, String product, String version, String message, long timestamp) {
}
