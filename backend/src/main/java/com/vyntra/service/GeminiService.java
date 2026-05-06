package com.vyntra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    public GeminiService(WebClient webClient) {
        this.webClient = webClient;
    }

    /** Generate text from a prompt using Gemini API */
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("demo")) {
            return fallbackResponse(prompt);
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;

            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode contents = body.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            // Generation config
            ObjectNode genConfig = body.putObject("generationConfig");
            genConfig.put("maxOutputTokens", 1024);
            genConfig.put("temperature", 0.7);

            String response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0).path("text");
            return text.asText("I'm having trouble generating a response right now.");
        } catch (Exception e) {
            log.warn("Gemini API error: {}", e.getMessage());
            return fallbackResponse(prompt);
        }
    }

    private String fallbackResponse(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("plan") || lower.contains("trip") || lower.contains("itinerary")) {
            return "I can help you plan your trip! Go to Plan Trip from the sidebar, enter your source and destination, pick your travel style, and I'll generate an optimized route with real places along the way.";
        }
        if (lower.contains("food") || lower.contains("eat") || lower.contains("restaurant")) {
            return "For food recommendations, I search real restaurants and cafes along your route using OpenStreetMap data. Look for the 'Nearby Food' button on each stop in your trip results!";
        }
        if (lower.contains("hotel") || lower.contains("stay") || lower.contains("accommodation")) {
            return "Hotel recommendations are shown in your trip results. Click 'Show More Hotels' to see additional stay options near your route.";
        }
        if (lower.contains("rescue") || lower.contains("emergency") || lower.contains("problem")) {
            return "Use Rescue Mode from the sidebar if you face any issues! It handles rain, road closures, low budget, fatigue, and more. It will instantly replan your route with safer alternatives.";
        }
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            return "Hello! 👋 I'm Vyntra AI. I can help with trip planning, route optimization, nearby place recommendations, and travel advice. What would you like to know?";
        }
        if (lower.contains("why") || lower.contains("how")) {
            return "Vyntra uses real geocoding, routing, and place data from OpenStreetMap to plan your trip. Places are scored by rating, distance from route, and your preferences to give you the best stops.";
        }
        return "I'm your Vyntra travel assistant! Ask me about trip planning, places to visit, food spots, hotels, or rescue mode help.";
    }
}
