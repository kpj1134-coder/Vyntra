package com.vyntra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiService(WebClient webClient) {
        this.webClient = webClient;
    }

    /** Generate text from a prompt using Gemini 2.0 Flash */
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equalsIgnoreCase("demo")) {
            log.warn("Gemini API key not configured — using fallback");
            return null; // Caller handles null as fallback
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode contents = body.putArray("contents");
            ObjectNode content = contents.addObject();
            content.put("role", "user");
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            ObjectNode config = body.putObject("generationConfig");
            config.put("maxOutputTokens", 800);
            config.put("temperature", 0.7);

            String response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(15))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
                if (!text.isBlank()) {
                    log.info("Gemini response received ({} chars)", text.length());
                    return text.trim();
                }
            }
            // Log the actual response for debugging
            log.warn("Gemini returned unexpected structure: {}", response.substring(0, Math.min(300, response.length())));
            return null;

        } catch (WebClientResponseException e) {
            log.error("Gemini API HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return null;
        }
    }

    /** Returns true if Gemini is configured and likely available */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equalsIgnoreCase("demo");
    }
}
