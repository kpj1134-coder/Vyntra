package com.vyntra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        try {
            mongoTemplate.getDb().getName();
            status.put("status", "UP");
            status.put("database", "MongoDB connected");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("database", "MongoDB connection failed: " + e.getMessage());
        }
        return ResponseEntity.ok(status);
    }
}
