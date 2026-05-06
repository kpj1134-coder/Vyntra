package com.vyntra.controller;

import com.vyntra.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String tripId = body.getOrDefault("tripId", "");

        String systemContext = "You are Vyntra AI, an intelligent travel assistant. "
                + "You help users plan trips, find nearby places, restaurants, hotels, attractions, and handle travel emergencies. "
                + "Be concise, friendly, and helpful. "
                + (tripId.isEmpty() ? "" : "The user is currently viewing trip ID: " + tripId + ". ");

        String reply = geminiService.generate(systemContext + "\n\nUser: " + message + "\n\nAssistant:");
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
