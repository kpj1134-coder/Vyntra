package com.vyntra.service;

import com.vyntra.dto.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiExplanationService {

    private final GeminiService geminiService;

    public AiExplanationService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public String generateExplanation(List<PlaceDTO> selected, List<RemovedPlaceDTO> removed,
                                       TripPlanRequest req, WeatherDTO weather, RouteDTO route) {
        if (selected == null || selected.isEmpty()) {
            return "No stops were found matching your criteria for this route.";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are Vyntra, an intelligent travel assistant. Create a concise, friendly travel itinerary summary.\n\n");
        prompt.append("Trip: ").append(req.getStartLocation()).append(" → ").append(req.getDestination()).append("\n");
        if (route != null) {
            prompt.append("Distance: ").append(route.getTotalDistanceKm()).append(" km, Duration: ").append(route.getTotalDuration()).append("\n");
        }
        prompt.append("Travel Group: ").append(req.getTravelType()).append(", Energy: ").append(req.getEnergyLevel()).append("\n");
        if (req.getTravelStyle() != null) prompt.append("Travel Style: ").append(req.getTravelStyle()).append("\n");
        if (weather != null) prompt.append("Weather: ").append(weather.getCondition()).append(", ").append(weather.getTemperature()).append("°C\n");
        prompt.append("\nSelected ").append(selected.size()).append(" stops:\n");
        for (int i = 0; i < selected.size(); i++) {
            PlaceDTO p = selected.get(i);
            prompt.append(i + 1).append(". ").append(p.getName())
                  .append(" (").append(p.getCategory()).append(")");
            if (p.getRating() != null) prompt.append(" ★").append(String.format("%.1f", p.getRating()));
            if (p.getEstimatedCost() != null) prompt.append(" ₹").append((int)Math.round(p.getEstimatedCost()));
            prompt.append("\n");
        }
        if (!removed.isEmpty()) {
            prompt.append("\nFiltered out ").append(removed.size()).append(" places due to constraints.\n");
        }
        prompt.append("\nWrite a 3-4 sentence travel plan summary. Be helpful and enthusiastic. Mention highlights and travel tips.");

        return geminiService.generate(prompt.toString());
    }

    public String generateRescueExplanation(List<PlaceDTO> places, String issue, String location) {
        String prompt = "You are Vyntra emergency travel assistant. A traveler near " + location
                + " has an issue: '" + issue + "'. "
                + "Found " + places.size() + " nearby safe options: "
                + places.stream().map(PlaceDTO::getName).collect(Collectors.joining(", "))
                + ". Write 2-3 sentences of emergency travel guidance and reassurance.";
        return geminiService.generate(prompt);
    }
}
