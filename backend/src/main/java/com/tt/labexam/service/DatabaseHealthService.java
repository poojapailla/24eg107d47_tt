package com.tt.labexam.service;

import com.tt.labexam.dto.DbHealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseHealthService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Runs a basic query against MySQL and reports the result without leaking connection details. */
    public DbHealthResponse check() {
        try {
            Integer probe = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            boolean connected = probe != null && probe == 1;
            String message = connected
                    ? "Database connection successful"
                    : "Unexpected probe result from database";
            return new DbHealthResponse(connected, "MySQL", version, message, System.currentTimeMillis());
        } catch (Exception ex) {
            // Only the exception type is logged so that connection strings and credentials stay out of the logs.
            log.warn("Database health check failed: {}", ex.getClass().getSimpleName());
            return new DbHealthResponse(false, "MySQL", null,
                    "Database connection failed. Verify SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME and SPRING_DATASOURCE_PASSWORD.",
                    System.currentTimeMillis());
        }
    }
}
