package com.tt.labexam.exception;

import com.tt.labexam.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns unexpected failures into safe JSON responses without stack traces or secrets. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Requests for a path that does not exist (for example Render polling "/") are normal
     * and must not be reported as server errors. Log them at debug level and answer 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("No handler for {} {}", request.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found: " + request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled error: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Unexpected server error"));
    }
}
