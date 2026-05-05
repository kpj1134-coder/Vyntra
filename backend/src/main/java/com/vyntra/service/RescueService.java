package com.vyntra.service;

import com.vyntra.dto.*;
import com.vyntra.model.*;
import com.vyntra.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RescueService {
    private final SerpApiService serpApiService;
    private final WeatherService weatherService;
    private final ConstraintEngineService constraintEngine;
    private final RouteOptimizationService optimizationService;
    private final AiExplanationService aiService;
    private final TripRepository tripRepo;
    private final SuggestedPlaceRepository placeRepo;
    private final RemovedPlaceRepository removedRepo;

    public RescueService(SerpApiService serpApiService, WeatherService weatherService,
            ConstraintEngineService constraintEngine, RouteOptimizationService optimizationService,
            AiExplanationService aiService, TripRepository tripRepo,
            SuggestedPlaceRepository placeRepo, RemovedPlaceRepository removedRepo) {
        this.serpApiService = serpApiService; this.weatherService = weatherService;
        this.constraintEngine = constraintEngine; this.optimizationService = optimizationService;
        this.aiService = aiService; this.tripRepo = tripRepo;
        this.placeRepo = placeRepo; this.removedRepo = removedRepo;
    }

    public TripPlanResponse rescue(RescueRequest request, User user) {
        WeatherDTO weather = weatherService.fetchWeather(request.getCurrentLocation());
        List<String> interests = request.getInterests();
        if (interests == null || interests.isEmpty()) interests = List.of("food", "rest", "emergency");

        // Use SerpApiService to dynamically get coordinates
        double[] coords = serpApiService.getCoordinates(request.getCurrentLocation());
        double lat = coords[0];
        double lng = coords[1];

        List<PlaceDTO> allPlaces = new ArrayList<>();
        for (String interest : interests) {
            String query = interest + " near me";
            allPlaces.addAll(serpApiService.searchPlaces(query, lat, lng));
        }

        // Remove duplicates
        Map<String, PlaceDTO> unique = new LinkedHashMap<>();
        for (PlaceDTO p : allPlaces) {
            String key = p.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            unique.putIfAbsent(key, p);
        }
        allPlaces = new ArrayList<>(unique.values());

        // Set default distance from current location
        for (PlaceDTO p : allPlaces) {
            if (p.getDistanceFromRoute() == null) {
                double d = SerpApiService.haversineDistance(lat, lng, p.getLatitude(), p.getLongitude());
                p.setDistanceFromRoute(Math.round(d * 100.0) / 100.0);
            }
        }

        // Apply constraints with rescue context
        ConstraintEngineService.FilterResult result = constraintEngine.applyConstraints(
            allPlaces, request.getCurrentEnergy(), "solo", request.getRemainingBudget(),
            request.getRemainingTime(), request.getIssue(), weather, interests);

        // Score and rank
        List<PlaceDTO> scored = optimizationService.scoreAndRank(
            result.kept, interests, weather, request.getCurrentEnergy());

        int maxStops = request.getRemainingTime() != null ? Math.min((int)(request.getRemainingTime() / 1.5), 4) : 3;
        List<PlaceDTO> selected = scored.stream().limit(Math.max(maxStops, 1)).collect(Collectors.toList());

        // Order stops
        for (int i = 0; i < selected.size(); i++) selected.get(i).setStopOrder(i + 1);

        // Build rescue plan request for AI
        TripPlanRequest planReq = new TripPlanRequest();
        planReq.setStartLocation(request.getCurrentLocation());
        String dest = request.getDestination() != null && !request.getDestination().isEmpty()
            ? request.getDestination() : "Nearby safe places";
        planReq.setDestination(dest);
        planReq.setBudget(request.getRemainingBudget());
        planReq.setTimeAvailable(request.getRemainingTime());
        planReq.setEnergyLevel(request.getCurrentEnergy());
        planReq.setTravelType("solo"); planReq.setMode("car");
        planReq.setInterests(interests);
        planReq.setProblemFaced(request.getIssue());

        String aiSummary = aiService.generateExplanation(selected, result.removed, planReq, weather, null);

        double totalCost = selected.stream().mapToDouble(p -> p.getEstimatedCost() != null ? p.getEstimatedCost() : 0).sum();
        String travelTime = maxStops * 45 + " mins";

        // Save rescue as a trip in DB so we get a tripId
        Trip trip = new Trip();
        trip.setUserId(user.getId());
        trip.setStartLocation(request.getCurrentLocation());
        trip.setDestination(dest);
        trip.setBudget(request.getRemainingBudget());
        trip.setTimeAvailable(request.getRemainingTime());
        trip.setEnergyLevel(request.getCurrentEnergy());
        trip.setTravelType("rescue");
        trip.setMode("car");
        trip.setInterests(interests);
        trip.setProblemFaced(request.getIssue());
        trip.setAiSummary(aiSummary);
        trip.setRouteSummary("Rescue plan from " + request.getCurrentLocation() + " → " + dest);
        trip.setWeatherSummary(weather != null ? weather.getCondition() + " " + weather.getTemperature() + "°C" : "N/A");
        trip.setEstimatedTotalCost(totalCost);
        trip.setEstimatedTravelTime(travelTime);
        trip.setFallback(true);
        trip = tripRepo.save(trip);

        // Save selected places
        for (PlaceDTO p : selected) {
            SuggestedPlace sp = new SuggestedPlace();
            sp.setTripId(trip.getId()); sp.setName(p.getName()); sp.setCategory(p.getCategory());
            sp.setAddress(p.getAddress()); sp.setRating(p.getRating()); sp.setEstimatedCost(p.getEstimatedCost());
            sp.setDistanceFromRoute(p.getDistanceFromRoute()); sp.setOpenNow(p.getOpenNow());
            sp.setLatitude(p.getLatitude()); sp.setLongitude(p.getLongitude());
            sp.setScore(p.getScore()); sp.setReason(p.getReason()); sp.setStopOrder(p.getStopOrder());
            placeRepo.save(sp);
        }
        for (RemovedPlaceDTO r : result.removed) {
            removedRepo.save(new RemovedPlace(trip.getId(), r.getName(), r.getReasonRemoved()));
        }

        TripPlanResponse response = new TripPlanResponse();
        response.setTripId(trip.getId());
        response.setStartLocation(request.getCurrentLocation());
        response.setDestination(dest);
        response.setSuggestedPlaces(selected);
        response.setRemovedPlaces(result.removed);
        response.setAiSummary(aiSummary);
        response.setEstimatedTotalCost(totalCost);
        response.setEstimatedTravelTime(travelTime);
        response.setWeather(weather);
        response.setDemoMode(true);
        response.setFallback(true);
        response.setRouteSummary("Rescue plan from " + request.getCurrentLocation() + " → " + dest);
        response.setWeatherSummary(weather != null ? weather.getCondition() + " " + weather.getTemperature() + "°C" : "N/A");
        return response;
    }
}
