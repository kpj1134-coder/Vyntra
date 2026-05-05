package com.vyntra.service;

import com.vyntra.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Service
public class AiExplanationService {
    private static final Logger log = LoggerFactory.getLogger(AiExplanationService.class);
    private final WebClient webClient;
    @Value("${ai.api.key}") private String aiApiKey;
    @Value("${ai.provider}") private String aiProvider;

    public AiExplanationService(WebClient webClient) { this.webClient = webClient; }

    public String generateExplanation(List<PlaceDTO> selected, List<RemovedPlaceDTO> removed,
            TripPlanRequest request, WeatherDTO weather, RouteDTO route) {
        String prompt = buildPrompt(selected, removed, request, weather, route);
        if ("demo".equals(aiApiKey)) return generateFallbackExplanation(selected, removed, request, weather, route);
        try {
            if ("gemini".equalsIgnoreCase(aiProvider)) return callGemini(prompt);
            else return callOpenAI(prompt);
        } catch (Exception e) {
            log.warn("AI API failed: {}", e.getMessage());
            return generateFallbackExplanation(selected, removed, request, weather, route);
        }
    }

    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + aiApiKey;
        Map<String, Object> body = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        content.put("parts", parts);
        contents.add(content);
        body.put("contents", contents);
        Map response = webClient.post().uri(url)
            .header("Content-Type", "application/json").bodyValue(body)
            .retrieve().bodyToMono(Map.class).block();
        if (response != null && response.containsKey("candidates")) {
            List<Map> candidates = (List<Map>) response.get("candidates");
            if (!candidates.isEmpty()) {
                Map c = (Map) candidates.get(0).get("content");
                List<Map> p = (List<Map>) c.get("parts");
                return (String) p.get(0).get("text");
            }
        }
        return generateFallbackFromPrompt(prompt);
    }

    private String callOpenAI(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-3.5-turbo");
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("max_tokens", 1000);
        Map response = webClient.post().uri(url)
            .header("Authorization", "Bearer " + aiApiKey)
            .header("Content-Type", "application/json").bodyValue(body)
            .retrieve().bodyToMono(Map.class).block();
        if (response != null && response.containsKey("choices")) {
            List<Map> choices = (List<Map>) response.get("choices");
            Map msg = (Map) choices.get(0).get("message");
            return (String) msg.get("content");
        }
        return generateFallbackFromPrompt(prompt);
    }

    private String buildPrompt(List<PlaceDTO> selected, List<RemovedPlaceDTO> removed,
            TripPlanRequest req, WeatherDTO weather, RouteDTO route) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are Vyntra, an intelligent travel assistant. Generate a clean itinerary summary.\n\n");
        sb.append("Trip: ").append(req.getStartLocation()).append(" to ").append(req.getDestination()).append("\n");
        sb.append("Budget: Rs.").append(fmt(req.getBudget())).append(" | Time: ").append(req.getTimeAvailable()).append("h\n");
        sb.append("Energy: ").append(req.getEnergyLevel()).append(" | Type: ").append(req.getTravelType()).append("\n");
        if (weather != null) sb.append("Weather: ").append(weather.getCondition()).append(" ").append(fmt(weather.getTemperature())).append(" C\n");
        if (route != null) sb.append("Route: ").append(fmt(route.getTotalDistanceKm())).append("km, ").append(route.getTotalDuration()).append("\n");
        sb.append("\nSelected stops:\n");
        for (PlaceDTO p : selected) sb.append("- ").append(p.getName()).append(" (").append(p.getCategory()).append(", score: ").append(fmt(p.getScore())).append(")\n");
        sb.append("\nRemoved places:\n");
        for (RemovedPlaceDTO r : removed) sb.append("- ").append(r.getName()).append(": ").append(r.getReasonRemoved()).append("\n");
        sb.append("\nProvide: 1) Clean itinerary 2) Why each stop 3) Safety tips 4) Budget summary 5) Time estimate");
        return sb.toString();
    }

    private String generateFallbackExplanation(List<PlaceDTO> sel, List<RemovedPlaceDTO> rem,
            TripPlanRequest req, WeatherDTO weather, RouteDTO route) {
        StringBuilder sb = new StringBuilder();
        sb.append("VYNTRA TRAVEL PLAN\n\n");
        sb.append("Route: ").append(req.getStartLocation()).append(" to ").append(req.getDestination()).append("\n");
        if (route != null) sb.append("Distance: ").append(fmt(route.getTotalDistanceKm())).append(" km | Duration: ").append(route.getTotalDuration()).append("\n");
        if (weather != null) sb.append("Weather: ").append(weather.getCondition()).append(" (").append(fmt(weather.getTemperature())).append(" C)\n\n");
        sb.append("YOUR OPTIMIZED STOPS\n\n");
        double totalCost = 0;
        for (int i = 0; i < sel.size(); i++) {
            PlaceDTO p = sel.get(i);
            sb.append("Stop ").append(i + 1).append(": ").append(p.getName()).append("\n");
            sb.append("  Category: ").append(p.getCategory()).append(" | Rating: ").append(fmt(p.getRating())).append("/5\n");
            sb.append("  Cost: Rs.").append(fmt(p.getEstimatedCost())).append(" | Deviation: ").append(fmt(p.getDistanceFromRoute())).append(" km\n");
            sb.append("  ").append(p.getReason()).append("\n\n");
            totalCost += p.getEstimatedCost() != null ? p.getEstimatedCost() : 0;
        }
        sb.append("BUDGET SUMMARY\n");
        sb.append("  Total estimated cost: Rs.").append(fmt(totalCost)).append("\n");
        double remaining = (req.getBudget() != null ? req.getBudget() : 0) - totalCost;
        sb.append("  Budget remaining: Rs.").append(fmt(remaining)).append("\n\n");
        sb.append("SAFETY TIPS\n");
        sb.append("  - Stay hydrated and carry water\n  - Keep emergency contacts handy\n");
        if (weather != null && weather.isRainy()) sb.append("  - Carry an umbrella - rain expected!\n");
        if (rem.size() > 0) {
            sb.append("\nPLACES REMOVED BY CONSTRAINTS\n");
            int limit = Math.min(rem.size(), 5);
            for (int i = 0; i < limit; i++) sb.append("  - ").append(rem.get(i).getName()).append(": ").append(rem.get(i).getReasonRemoved()).append("\n");
            if (rem.size() > 5) sb.append("  ... and ").append(rem.size() - 5).append(" more\n");
        }
        return sb.toString();
    }

    private String generateFallbackFromPrompt(String prompt) { return "AI summary temporarily unavailable."; }

    private String fmt(Double val) {
        if (val == null) return "0";
        return String.format("%.1f", val);
    }
}
