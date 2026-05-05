package com.vyntra.service;

import com.vyntra.dto.PlaceDTO;
import com.vyntra.dto.RemovedPlaceDTO;
import com.vyntra.dto.WeatherDTO;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConstraintEngineService {

    public static class FilterResult {
        public List<PlaceDTO> kept;
        public List<RemovedPlaceDTO> removed;
        public FilterResult(List<PlaceDTO> kept, List<RemovedPlaceDTO> removed) {
            this.kept = kept; this.removed = removed;
        }
    }

    public FilterResult applyConstraints(List<PlaceDTO> places, String energyLevel, String travelType,
            Double budget, Double timeAvailable, String problemFaced, WeatherDTO weather, List<String> interests) {

        List<PlaceDTO> kept = new ArrayList<>(places);
        List<RemovedPlaceDTO> removed = new ArrayList<>();

        // Rain constraint
        if (weather != null && weather.isRainy()) {
            kept = applyRainFilter(kept, removed);
        }
        if ("rain".equalsIgnoreCase(problemFaced)) {
            kept = applyRainFilter(kept, removed);
        }

        // Budget constraint
        if (budget != null && budget < 500) {
            kept = applyLowBudgetFilter(kept, removed, budget);
        }
        if ("low budget".equalsIgnoreCase(problemFaced) || "low_budget".equalsIgnoreCase(problemFaced)) {
            kept = applyLowBudgetFilter(kept, removed, budget != null ? budget : 300);
        }

        // Energy constraint
        if ("low".equalsIgnoreCase(energyLevel)) {
            kept = applyLowEnergyFilter(kept, removed);
        }
        if ("tired".equalsIgnoreCase(problemFaced)) {
            kept = applyLowEnergyFilter(kept, removed);
        }

        // Time constraint
        if (timeAvailable != null && timeAvailable < 3) {
            kept = applyTimeLimitFilter(kept, removed, timeAvailable);
        }

        // Closed places
        if ("closed place".equalsIgnoreCase(problemFaced) || "closed_place".equalsIgnoreCase(problemFaced)) {
            kept = removeClosedPlaces(kept, removed);
        }

        // Family filter
        if ("family".equalsIgnoreCase(travelType)) {
            kept = applyFamilyFilter(kept, removed);
        }

        // Route deviation filter - keep within 2km
        kept = applyDeviationFilter(kept, removed, 2.0);

        return new FilterResult(kept, removed);
    }

    private List<PlaceDTO> applyRainFilter(List<PlaceDTO> places, List<RemovedPlaceDTO> removed) {
        Set<String> outdoor = Set.of("nature", "park", "lake", "viewpoint", "garden", "beach");
        List<PlaceDTO> filtered = new ArrayList<>();
        for (PlaceDTO p : places) {
            String cat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
            boolean isOutdoor = outdoor.stream().anyMatch(cat::contains);
            String name = p.getName() != null ? p.getName().toLowerCase() : "";
            isOutdoor = isOutdoor || name.contains("lake") || name.contains("park") || name.contains("garden") || name.contains("viewpoint");
            if (isOutdoor) {
                removed.add(new RemovedPlaceDTO(p.getName(), "Removed: outdoor place not suitable during rain"));
            } else {
                filtered.add(p);
            }
        }
        return filtered;
    }

    private List<PlaceDTO> applyLowBudgetFilter(List<PlaceDTO> places, List<RemovedPlaceDTO> removed, double budget) {
        double perPlaceBudget = budget / Math.max(places.size(), 1);
        List<PlaceDTO> filtered = new ArrayList<>();
        for (PlaceDTO p : places) {
            double cost = p.getEstimatedCost() != null ? p.getEstimatedCost() : 0;
            if (cost > perPlaceBudget && cost > 300) {
                removed.add(new RemovedPlaceDTO(p.getName(), "Removed: exceeds budget (cost ₹" + cost + ")"));
            } else {
                filtered.add(p);
            }
        }
        return filtered;
    }

    private List<PlaceDTO> applyLowEnergyFilter(List<PlaceDTO> places, List<RemovedPlaceDTO> removed) {
        Set<String> highEnergy = Set.of("trek", "hike", "adventure", "sports", "climb");
        List<PlaceDTO> filtered = new ArrayList<>();
        for (PlaceDTO p : places) {
            String name = p.getName() != null ? p.getName().toLowerCase() : "";
            String cat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
            boolean isHighEnergy = highEnergy.stream().anyMatch(h -> name.contains(h) || cat.contains(h));
            double dist = p.getDistanceFromRoute() != null ? p.getDistanceFromRoute() : 0;
            if (isHighEnergy || dist > 1.5) {
                removed.add(new RemovedPlaceDTO(p.getName(), "Removed: requires too much energy or is too far (" + dist + " km)"));
            } else {
                filtered.add(p);
            }
        }
        return filtered;
    }

    private List<PlaceDTO> applyTimeLimitFilter(List<PlaceDTO> places, List<RemovedPlaceDTO> removed, double time) {
        int maxStops = time < 1.5 ? 1 : 2;
        places.sort(Comparator.comparingDouble(p -> p.getDistanceFromRoute() != null ? p.getDistanceFromRoute() : 99));
        List<PlaceDTO> filtered = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            if (i < maxStops) { filtered.add(places.get(i)); }
            else { removed.add(new RemovedPlaceDTO(places.get(i).getName(), "Removed: time too limited for extra stop")); }
        }
        return filtered;
    }

    private List<PlaceDTO> removeClosedPlaces(List<PlaceDTO> places, List<RemovedPlaceDTO> removed) {
        List<PlaceDTO> filtered = new ArrayList<>();
        for (PlaceDTO p : places) {
            if (p.getOpenNow() != null && !p.getOpenNow()) {
                removed.add(new RemovedPlaceDTO(p.getName(), "Removed: currently closed"));
            } else {
                filtered.add(p);
            }
        }
        return filtered;
    }

    private List<PlaceDTO> applyFamilyFilter(List<PlaceDTO> places, List<RemovedPlaceDTO> removed) {
        Set<String> unsafe = Set.of("bar", "pub", "nightclub", "brewery", "lounge");
        List<PlaceDTO> filtered = new ArrayList<>();
        for (PlaceDTO p : places) {
            String name = p.getName() != null ? p.getName().toLowerCase() : "";
            String cat = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
            boolean isUnsafe = unsafe.stream().anyMatch(u -> name.contains(u) || cat.contains(u));
            if (isUnsafe) {
                removed.add(new RemovedPlaceDTO(p.getName(), "Removed: not family-friendly"));
            } else {
                filtered.add(p);
            }
        }
        return filtered;
    }

    private List<PlaceDTO> applyDeviationFilter(List<PlaceDTO> places, List<RemovedPlaceDTO> removed, double maxKm) {
        List<PlaceDTO> filtered = new ArrayList<>();
        for (PlaceDTO p : places) {
            double dist = p.getDistanceFromRoute() != null ? p.getDistanceFromRoute() : 0;
            if (dist > maxKm) {
                removed.add(new RemovedPlaceDTO(p.getName(), "Removed: too far from route (" + dist + " km deviation)"));
            } else {
                filtered.add(p);
            }
        }
        return filtered;
    }
}
