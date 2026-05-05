package com.vyntra.service;

import com.vyntra.dto.PlaceDTO;
import com.vyntra.dto.WeatherDTO;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RouteOptimizationService {

    /**
     * Score each place using the weighted formula:
     * score = (rating*20) - (deviationKm*15) - (estimatedCost*0.05)
     *       + interestMatchBonus + weatherSuitabilityBonus + openNowBonus - energyPenalty
     */
    public List<PlaceDTO> scoreAndRank(List<PlaceDTO> places, List<String> interests,
            WeatherDTO weather, String energyLevel) {
        for (PlaceDTO p : places) {
            double score = 0;
            double rating = p.getRating() != null ? p.getRating() : 3.0;
            double deviation = p.getDistanceFromRoute() != null ? p.getDistanceFromRoute() : 1.0;
            double cost = p.getEstimatedCost() != null ? p.getEstimatedCost() : 0;

            score += rating * 20;
            score -= deviation * 15;
            score -= cost * 0.05;

            // Interest match bonus
            if (interests != null && p.getCategory() != null) {
                for (String interest : interests) {
                    if (p.getCategory().toLowerCase().contains(interest.toLowerCase()) ||
                        (p.getName() != null && p.getName().toLowerCase().contains(interest.toLowerCase()))) {
                        score += 25;
                        break;
                    }
                }
            }

            // Weather suitability bonus
            if (weather != null && weather.isRainy()) {
                String cat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
                if (cat.contains("museum") || cat.contains("food") || cat.contains("hotel") ||
                    cat.contains("shopping") || cat.contains("rest") || cat.contains("temple")) {
                    score += 15;
                }
            } else {
                score += 5; // neutral weather bonus
            }

            // Open now bonus
            if (p.getOpenNow() != null && p.getOpenNow()) score += 10;

            // Energy penalty
            if ("low".equalsIgnoreCase(energyLevel)) {
                if (deviation > 1.0) score -= 20;
                String cat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
                if (cat.contains("rest") || cat.contains("food") || cat.contains("cafe")) score += 10;
            }

            p.setScore(Math.round(score * 100.0) / 100.0);
            p.setReason(generateReason(p, interests, weather, energyLevel));
        }

        places.sort(Comparator.comparingDouble(PlaceDTO::getScore).reversed());
        return places;
    }

    /**
     * Optimize stop order using nearest-neighbor heuristic (Dijkstra-inspired).
     * Minimizes total travel distance from start through stops to destination.
     */
    public List<PlaceDTO> optimizeStopOrder(List<PlaceDTO> places, double startLat, double startLng,
            double destLat, double destLng) {
        if (places.size() <= 1) {
            if (!places.isEmpty()) places.get(0).setStopOrder(1);
            return places;
        }

        List<PlaceDTO> remaining = new ArrayList<>(places);
        List<PlaceDTO> ordered = new ArrayList<>();
        double curLat = startLat, curLng = startLng;

        while (!remaining.isEmpty()) {
            PlaceDTO nearest = null;
            double minDist = Double.MAX_VALUE;

            for (PlaceDTO p : remaining) {
                double dist = SerpApiService.haversineDistance(curLat, curLng, p.getLatitude(), p.getLongitude());
                // Add penalty for going away from destination
                double distToDest = SerpApiService.haversineDistance(p.getLatitude(), p.getLongitude(), destLat, destLng);
                double totalCost = dist + distToDest * 0.3;
                if (totalCost < minDist) { minDist = totalCost; nearest = p; }
            }

            if (nearest != null) {
                ordered.add(nearest);
                curLat = nearest.getLatitude();
                curLng = nearest.getLongitude();
                remaining.remove(nearest);
            }
        }

        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setStopOrder(i + 1);
        }
        return ordered;
    }

    private String generateReason(PlaceDTO p, List<String> interests, WeatherDTO weather, String energy) {
        StringBuilder reason = new StringBuilder();
        reason.append(p.getName()).append(" was selected because ");
        List<String> reasons = new ArrayList<>();

        if (p.getDistanceFromRoute() != null)
            reasons.add("it is only " + p.getDistanceFromRoute() + " km from your route");
        if (p.getRating() != null && p.getRating() >= 4.0)
            reasons.add("it has a high rating of " + p.getRating());
        if (p.getEstimatedCost() != null && p.getEstimatedCost() == 0)
            reasons.add("it is free to visit");
        if (p.getOpenNow() != null && p.getOpenNow())
            reasons.add("it is currently open");
        if (weather != null && weather.isRainy() && p.getCategory() != null) {
            String cat = p.getCategory().toLowerCase();
            if (cat.contains("museum") || cat.contains("food") || cat.contains("temple"))
                reasons.add("it is suitable during rain");
        }
        if ("low".equalsIgnoreCase(energy) && p.getCategory() != null) {
            String cat = p.getCategory().toLowerCase();
            if (cat.contains("rest") || cat.contains("food") || cat.contains("cafe"))
                reasons.add("it fits your low-energy preference");
        }
        if (interests != null && p.getCategory() != null) {
            for (String i : interests) {
                if (p.getCategory().toLowerCase().contains(i.toLowerCase())) {
                    reasons.add("it matches your interest in " + i);
                    break;
                }
            }
        }

        if (reasons.isEmpty()) reasons.add("it scored well on our optimization criteria");
        reason.append(String.join(", ", reasons)).append(".");
        return reason.toString();
    }
}
