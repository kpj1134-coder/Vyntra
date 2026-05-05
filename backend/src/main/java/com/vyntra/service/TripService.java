package com.vyntra.service;

import com.vyntra.dto.*;
import com.vyntra.model.*;
import com.vyntra.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TripService {
    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private final TripRepository tripRepo;
    private final SuggestedPlaceRepository placeRepo;
    private final RemovedPlaceRepository removedRepo;
    private final SavedItineraryRepository itineraryRepo;
    private final SerpApiService serpApiService;
    private final WeatherService weatherService;
    private final ConstraintEngineService constraintEngine;
    private final RouteOptimizationService optimizationService;
    private final AiExplanationService aiService;
    private final FallbackDataService fallbackDataService;
    private final ObjectMapper objectMapper;

    public TripService(TripRepository tripRepo, SuggestedPlaceRepository placeRepo,
            RemovedPlaceRepository removedRepo, SavedItineraryRepository itineraryRepo,
            SerpApiService serpApiService, WeatherService weatherService,
            ConstraintEngineService constraintEngine, RouteOptimizationService optimizationService,
            AiExplanationService aiService, FallbackDataService fallbackDataService,
            ObjectMapper objectMapper) {
        this.tripRepo = tripRepo; this.placeRepo = placeRepo; this.removedRepo = removedRepo;
        this.itineraryRepo = itineraryRepo; this.serpApiService = serpApiService;
        this.weatherService = weatherService; this.constraintEngine = constraintEngine;
        this.optimizationService = optimizationService; this.aiService = aiService;
        this.fallbackDataService = fallbackDataService; this.objectMapper = objectMapper;
    }

    public TripPlanResponse planTrip(TripPlanRequest request, User user) {
        // 1. Get route
        RouteDTO route = serpApiService.fetchDirections(request.getStartLocation(), request.getDestination());

        // 2. Get weather
        WeatherDTO weather = weatherService.fetchWeather(request.getDestination());

        // 3. Discover places along route
        List<PlaceDTO> allPlaces;
        if (fallbackDataService.isMysoreDemo(request.getStartLocation(), request.getDestination())) {
            // Use curated Mysore demo data
            allPlaces = fallbackDataService.getMysoreDemo();
        } else {
            allPlaces = discoverPlacesAlongRoute(route, request.getInterests());
        }

        // 4. Remove duplicates
        allPlaces = removeDuplicates(allPlaces);

        // 5. Calculate distance from route for each place
        if (route.getWaypoints() != null && !route.getWaypoints().isEmpty()) {
            for (PlaceDTO p : allPlaces) {
                if (p.getDistanceFromRoute() == null) {
                    double minDist = Double.MAX_VALUE;
                    for (double[] wp : route.getWaypoints()) {
                        double d = SerpApiService.haversineDistance(wp[0], wp[1], p.getLatitude(), p.getLongitude());
                        if (d < minDist) minDist = d;
                    }
                    p.setDistanceFromRoute(round2(minDist));
                }
            }
        }

        // 6. Apply constraint engine
        String problem = request.getProblemFaced() != null ? request.getProblemFaced() : "no problem";
        ConstraintEngineService.FilterResult filterResult = constraintEngine.applyConstraints(
            allPlaces, request.getEnergyLevel(), request.getTravelType(),
            request.getBudget(), request.getTimeAvailable(), problem, weather, request.getInterests());

        // 7. Score and rank
        List<PlaceDTO> scored = optimizationService.scoreAndRank(
            filterResult.kept, request.getInterests(), weather, request.getEnergyLevel());

        // 8. Limit to reasonable number of stops
        int maxStops = calculateMaxStops(request.getTimeAvailable());
        List<PlaceDTO> selected = scored.stream().limit(maxStops).collect(Collectors.toList());
        for (int i = maxStops; i < scored.size(); i++) {
            filterResult.removed.add(new RemovedPlaceDTO(scored.get(i).getName(), "Lower priority - enough stops selected"));
        }

        // 9. Optimize stop order
        double startLat = route.getWaypoints() != null && !route.getWaypoints().isEmpty() ? route.getWaypoints().get(0)[0] : 12.9716;
        double startLng = route.getWaypoints() != null && !route.getWaypoints().isEmpty() ? route.getWaypoints().get(0)[1] : 77.5946;
        double destLat = route.getWaypoints() != null && !route.getWaypoints().isEmpty() ? route.getWaypoints().get(route.getWaypoints().size()-1)[0] : 12.9716;
        double destLng = route.getWaypoints() != null && !route.getWaypoints().isEmpty() ? route.getWaypoints().get(route.getWaypoints().size()-1)[1] : 77.5946;
        selected = optimizationService.optimizeStopOrder(selected, startLat, startLng, destLat, destLng);

        // Round numeric values for clean display
        for (PlaceDTO p : selected) {
            if (p.getRating() != null) p.setRating(round2(p.getRating()));
            if (p.getEstimatedCost() != null) p.setEstimatedCost(round2(p.getEstimatedCost()));
            if (p.getScore() != null) p.setScore(round2(p.getScore()));
        }

        // 10. AI explanation
        String aiSummary = aiService.generateExplanation(selected, filterResult.removed, request, weather, route);

        // 11. Build response values
        double totalCost = round2(selected.stream().mapToDouble(p -> p.getEstimatedCost() != null ? p.getEstimatedCost() : 0).sum());
        String travelTime = route.getTotalDuration();
        String routeSummary = request.getStartLocation() + " to " + request.getDestination() +
            " (" + (route.getTotalDistanceKm() != null ? route.getTotalDistanceKm() : "N/A") + " km)";
        String weatherSummary = weather != null ?
            weather.getCondition() + " " + weather.getTemperature() + " C" : "Weather data unavailable";
        boolean isFallback = fallbackDataService.isMysoreDemo(request.getStartLocation(), request.getDestination());

        // 12. Save to database
        Trip trip = saveTrip(request, user, selected, filterResult.removed, aiSummary,
            routeSummary, weatherSummary, totalCost, travelTime, isFallback);

        // 13. Build response
        TripPlanResponse response = new TripPlanResponse();
        response.setTripId(trip.getId());
        response.setStartLocation(request.getStartLocation());
        response.setDestination(request.getDestination());
        response.setSuggestedPlaces(selected);
        response.setRemovedPlaces(filterResult.removed);
        response.setAiSummary(aiSummary);
        response.setEstimatedTotalCost(totalCost);
        response.setEstimatedTravelTime(travelTime);
        response.setWeather(weather);
        response.setRoute(route);
        response.setDemoMode(isFallback);
        response.setRouteSummary(routeSummary);
        response.setWeatherSummary(weatherSummary);
        response.setFallback(isFallback);
        return response;
    }

    private List<PlaceDTO> discoverPlacesAlongRoute(RouteDTO route, List<String> interests) {
        List<PlaceDTO> all = new ArrayList<>();
        List<double[]> waypoints = route.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) return all;

        Set<String> searchCategories = new HashSet<>();
        if (interests != null) {
            for (String interest : interests) {
                searchCategories.addAll(getSearchQueriesForInterest(interest));
            }
        }
        if (searchCategories.isEmpty()) searchCategories.addAll(List.of("restaurants", "cafes", "temples"));

        // Search at route segment midpoints (every 2-3 waypoints)
        int step = Math.max(1, waypoints.size() / 3);
        for (int i = 0; i < waypoints.size(); i += step) {
            double[] wp = waypoints.get(i);
            for (String query : searchCategories) {
                List<PlaceDTO> found = serpApiService.searchPlaces(query, wp[0], wp[1]);
                all.addAll(found);
            }
        }
        return all;
    }

    private List<String> getSearchQueriesForInterest(String interest) {
        switch (interest.toLowerCase()) {
            case "food": return List.of("restaurants", "cafes");
            case "nature": return List.of("parks", "viewpoints");
            case "temple": return List.of("temples", "shrines");
            case "shopping": return List.of("malls", "markets");
            case "hotel": return List.of("hotels", "lodges");
            case "rest": return List.of("cafes", "rest stops");
            case "emergency": return List.of("hospital", "pharmacy");
            case "museum": return List.of("museums", "historical places");
            default: return List.of(interest + " near me");
        }
    }

    private List<PlaceDTO> removeDuplicates(List<PlaceDTO> places) {
        Map<String, PlaceDTO> unique = new LinkedHashMap<>();
        for (PlaceDTO p : places) {
            String key = p.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            if (!unique.containsKey(key)) unique.put(key, p);
        }
        return new ArrayList<>(unique.values());
    }

    private int calculateMaxStops(Double timeAvailable) {
        if (timeAvailable == null) return 4;
        if (timeAvailable <= 2) return 1;
        if (timeAvailable <= 3) return 2;
        if (timeAvailable <= 5) return 4;
        if (timeAvailable <= 8) return 5;
        return 6;
    }

    private Trip saveTrip(TripPlanRequest req, User user, List<PlaceDTO> selected,
            List<RemovedPlaceDTO> removed, String aiSummary, String routeSummary,
            String weatherSummary, double totalCost, String travelTime, boolean fallback) {
        Trip trip = new Trip();
        trip.setUserId(user.getId());
        trip.setStartLocation(req.getStartLocation());
        trip.setDestination(req.getDestination());
        trip.setBudget(req.getBudget());
        trip.setTimeAvailable(req.getTimeAvailable());
        trip.setEnergyLevel(req.getEnergyLevel());
        trip.setTravelType(req.getTravelType());
        trip.setMode(req.getMode());
        trip.setInterests(req.getInterests());
        trip.setProblemFaced(req.getProblemFaced());
        trip.setAiSummary(aiSummary);
        trip.setRouteSummary(routeSummary);
        trip.setWeatherSummary(weatherSummary);
        trip.setEstimatedTotalCost(totalCost);
        trip.setEstimatedTravelTime(travelTime);
        trip.setFallback(fallback);
        trip = tripRepo.save(trip);

        for (PlaceDTO p : selected) {
            SuggestedPlace sp = new SuggestedPlace();
            sp.setTripId(trip.getId()); sp.setName(p.getName()); sp.setCategory(p.getCategory());
            sp.setAddress(p.getAddress()); sp.setRating(p.getRating()); sp.setEstimatedCost(p.getEstimatedCost());
            sp.setDistanceFromRoute(p.getDistanceFromRoute()); sp.setOpenNow(p.getOpenNow());
            sp.setLatitude(p.getLatitude()); sp.setLongitude(p.getLongitude());
            sp.setScore(p.getScore()); sp.setReason(p.getReason()); sp.setStopOrder(p.getStopOrder());
            placeRepo.save(sp);
        }
        for (RemovedPlaceDTO r : removed) {
            removedRepo.save(new RemovedPlace(trip.getId(), r.getName(), r.getReasonRemoved()));
        }

        // Auto-save itinerary
        SavedItinerary saved = new SavedItinerary();
        saved.setTripId(trip.getId());
        saved.setUserId(user.getId());
        saved.setAiSummary(aiSummary);
        try { saved.setItineraryJson(objectMapper.writeValueAsString(selected)); } catch (Exception e) { saved.setItineraryJson("[]"); }
        itineraryRepo.save(saved);

        return trip;
    }

    public TripPlanResponse getTripById(String tripId, User user) {
        Trip trip = tripRepo.findById(tripId).orElseThrow(() -> new RuntimeException("Trip not found"));
        if (!trip.getUserId().equals(user.getId())) throw new RuntimeException("Unauthorized");

        List<SuggestedPlace> sps = placeRepo.findByTripIdOrderByStopOrderAsc(tripId);
        List<RemovedPlace> rps = removedRepo.findByTripId(tripId);

        TripPlanResponse resp = new TripPlanResponse();
        resp.setTripId(trip.getId());
        resp.setStartLocation(trip.getStartLocation());
        resp.setDestination(trip.getDestination());
        resp.setSuggestedPlaces(sps.stream().map(this::toPlaceDTO).collect(Collectors.toList()));
        resp.setRemovedPlaces(rps.stream().map(r -> new RemovedPlaceDTO(r.getName(), r.getReasonRemoved())).collect(Collectors.toList()));
        resp.setAiSummary(trip.getAiSummary());
        resp.setEstimatedTotalCost(trip.getEstimatedTotalCost() != null ? trip.getEstimatedTotalCost() :
            sps.stream().mapToDouble(p -> p.getEstimatedCost() != null ? p.getEstimatedCost() : 0).sum());
        resp.setEstimatedTravelTime(trip.getEstimatedTravelTime());
        resp.setRouteSummary(trip.getRouteSummary());
        resp.setWeatherSummary(trip.getWeatherSummary());
        resp.setDemoMode(trip.isFallback());
        resp.setFallback(trip.isFallback());
        return resp;
    }

    public List<TripHistoryDTO> getTripHistory(User user) {
        return tripRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(t -> {
            TripHistoryDTO dto = new TripHistoryDTO();
            dto.setId(t.getId()); dto.setStartLocation(t.getStartLocation());
            dto.setDestination(t.getDestination()); dto.setBudget(t.getBudget());
            dto.setTimeAvailable(t.getTimeAvailable()); dto.setEnergyLevel(t.getEnergyLevel());
            dto.setTravelType(t.getTravelType()); dto.setMode(t.getMode());
            dto.setCreatedAt(t.getCreatedAt());
            dto.setPlacesCount(placeRepo.findByTripIdOrderByStopOrderAsc(t.getId()).size());
            dto.setHasSavedItinerary(itineraryRepo.findByTripId(t.getId()).isPresent());
            return dto;
        }).collect(Collectors.toList());
    }

    public void saveItinerary(String tripId, User user) {
        Trip trip = tripRepo.findById(tripId).orElseThrow(() -> new RuntimeException("Trip not found"));
        if (!trip.getUserId().equals(user.getId())) throw new RuntimeException("Unauthorized");

        TripPlanResponse resp = getTripById(tripId, user);
        SavedItinerary saved = itineraryRepo.findByTripId(tripId).orElse(new SavedItinerary());
        saved.setTripId(trip.getId());
        saved.setUserId(user.getId());
        try { saved.setItineraryJson(objectMapper.writeValueAsString(resp)); } catch (Exception e) { saved.setItineraryJson("{}"); }
        saved.setAiSummary(resp.getAiSummary());
        itineraryRepo.save(saved);
    }

    private PlaceDTO toPlaceDTO(SuggestedPlace sp) {
        PlaceDTO dto = new PlaceDTO();
        dto.setId(sp.getId()); dto.setName(sp.getName()); dto.setCategory(sp.getCategory());
        dto.setAddress(sp.getAddress()); dto.setRating(sp.getRating()); dto.setEstimatedCost(sp.getEstimatedCost());
        dto.setDistanceFromRoute(sp.getDistanceFromRoute()); dto.setOpenNow(sp.getOpenNow());
        dto.setLatitude(sp.getLatitude()); dto.setLongitude(sp.getLongitude());
        dto.setScore(sp.getScore()); dto.setReason(sp.getReason()); dto.setStopOrder(sp.getStopOrder());
        return dto;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
