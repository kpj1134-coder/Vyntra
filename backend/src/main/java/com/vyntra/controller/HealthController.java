package com.vyntra.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Root and health-check endpoints — publicly accessible.
 * Prevents 403 when Render or browser hits the root URL.
 */
@RestController
public class HealthController {

    /** Root URL — confirms backend is live */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
            "app",     "Vyntra Intelligent Travel System",
            "status",  "running",
            "version", "2.0.0",
            "health",  "/api/health"
        ));
    }

    /** Health check — used by Render and monitoring tools */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status",  "ok",
            "service", "vyntra-backend"
        ));
    }
}
