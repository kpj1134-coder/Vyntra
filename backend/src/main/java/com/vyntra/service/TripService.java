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
    private final NominatimService nominatimService;
    private final OsrmRouteService osrmRouteService;
    private final OverpassApiService overpassApiService;
    private final WeatherService weatherService;
    private final ConstraintEngineService constraintEngine;
    private final RouteOptimizationService optimizationService;
    private final AiExplanationService aiService;
    private final ObjectMapper objectMapper;

    public TripService(TripRepository tripRepo, SuggestedPlaceRepository placeRepo,
            RemovedPlaceRepository removedRepo, SavedItineraryRepository itineraryRepo,
            NominatimService nominatimService, OsrmRouteService osrmRouteService,
            OverpassApiService overpassApiService, WeatherService weatherService,
            ConstraintEngineService constraintEngine, RouteOptimizationService optimizationService,
            AiExplanationService aiService, ObjectMapper objectMapper) {
        this.tripRepo = tripRepo; this.placeRepo = placeRepo;
        this.removedRepo = removedRepo; this.itineraryRepo = itineraryRepo;
        this.nominatimService = nominatimService; this.osrmRouteService = osrmRouteService;
        this.overpassApiService = overpassApiService; this.weatherService = weatherService;
        this.constraintEngine = constraintEngine; this.optimizationService = optimizationService;
        this.aiService = aiService; this.objectMapper = objectMapper;
    }

    public TripPlanResponse planTrip(TripPlanRequest request, User user) {
        // 1. Geocode start and destination
        double[] startCoords = nominatimService.geocode(request.getStartLocation());
        double[] endCoords   = nominatimService.geocode(request.getDestination());
        log.info("Start: {} -> [{}, {}]", request.getStartLocation(), startCoords[0], startCoords[1]);
        log.info("End:   {} -> [{}, {}]", request.getDestination(), endCoords[0], endCoords[1]);

        // 2. Get real route via OSRM
        RouteDTO route = osrmRouteService.getRoute(startCoords[0], startCoords[1], endCoords[0], endCoords[1]);

        // 3. Get weather
        WeatherDTO weather = weatherService.fetchWeather(request.getDestination());

        // 4. Discover real places along route using Overpass API
        List<PlaceDTO> allPlaces = discoverRealPlaces(route, request);

        // 5. Remove duplicates
        allPlaces = removeDuplicates(allPlaces);

        // 6. Calculate distance from route for each place
        setDistancesFromRoute(allPlaces, route);

        // 7. Apply constraint engine
        String problem = request.getProblemFaced() != null ? request.getProblemFaced() : "";
        ConstraintEngineService.FilterResult filterResult = constraintEngine.applyConstraints(
            allPlaces, request.getEnergyLevel(), request.getTravelType(),
            request.getBudget(), request.getTimeAvailable(), problem, weather, request.getInterests());

        // 8. Score and rank
        List<PlaceDTO> scored = optimizationService.scoreAndRank(
            filterResult.kept, request.getInterests(), weather, request.getEnergyLevel());

        // 9. Limit stops
        int maxStops = calculateMaxStops(request.getTimeAvailable());
        List<PlaceDTO> selected = scored.stream().limit(maxStops).collect(Collectors.toList());
        for (int i = maxStops; i < scored.size(); i++) {
            filterResult.removed.add(new RemovedPlaceDTO(scored.get(i).getName(), "Lower priority - enough stops selected"));
        }

        // 10. Optimize order
        double sLat = startCoords[0], sLon = startCoords[1];
        double eLat = endCoords[0], eLon = endCoords[1];
        selected = optimizationService.optimizeStopOrder(selected, sLat, sLon, eLat, eLon);

        // Round values
        for (PlaceDTO p : selected) {
            if (p.getRating() != null) p.setRating(round2(p.getRating()));
            if (p.getEstimatedCost() != null) p.setEstimatedCost(round2(p.getEstimatedCost()));
            if (p.getScore() != null) p.setScore(round2(p.getScore()));
        }

        // 11. AI explanation
        String aiSummary = aiService.generateExplanation(selected, filterResult.removed, request, weather, route);

        // 12. Build response values
        double totalCost = round2(selected.stream().mapToDouble(p -> p.getEstimatedCost() != null ? p.getEstimatedCost() : 0).sum());
        String routeSummary = request.getStartLocation() + " → " + request.getDestination()
            + " (" + (route.getTotalDistanceKm() != null ? route.getTotalDistanceKm() : "N/A") + " km)";
        String weatherSummary = weather != null ? weather.getCondition() + " " + weather.getTemperature() + "°C" : "Weather unavailable";

        // 13. Save to MongoDB
        Trip trip = saveTrip(request, user, selected, filterResult.removed, aiSummary,
            routeSummary, weatherSummary, totalCost, route.getTotalDuration(), false);

        // 14. Build and return response
        TripPlanResponse response = new TripPlanResponse();
        response.setTripId(trip.getId());
        response.setStartLocation(request.getStartLocation());
        response.setDestination(request.getDestination());
        response.setSuggestedPlaces(selected);
        response.setRemovedPlaces(filterResult.removed);
        response.setAiSummary(aiSummary);
        response.setEstimatedTotalCost(totalCost);
        response.setEstimatedTravelTime(route.getTotalDuration());
        response.setWeather(weather);
        response.setRoute(route);
        response.setDemoMode(false);
        response.setFallback(false);
        response.setRouteSummary(routeSummary);
        response.setWeatherSummary(weatherSummary);
        return response;
    }

    /** Discover real places along route - FAST mode: 1 midpoint + max 2 categories */
    private List<PlaceDTO> discoverRealPlaces(RouteDTO route, TripPlanRequest req) {
        List<PlaceDTO> all = new ArrayList<>();
        List<double[]> waypoints = route.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) return generateFallbackPlaces(new ArrayList<>(), List.of("food", "attraction"));

        // Resolve categories but cap at 2 to keep response fast
        List<String> categories = resolveCategories(req.getInterests(), req.getTravelStyle());
        if (categories.size() > 2) categories = categories.subList(0, 2);
        log.info("Fast search: categories={} at midpoint", categories);

        // Search ONLY at the midpoint of the route (fastest single call)
        int midIdx = waypoints.size() / 2;
        double[] midpoint = waypoints.get(midIdx);

        // Single combined Overpass query for all categories at midpoint
        for (String cat : categories) {
            try {
                List<PlaceDTO> found = overpassApiService.searchNearby(cat, midpoint[0], midpoint[1], 8000);
                all.addAll(found);
                // No sleep — be fast
            } catch (Exception e) {
                log.warn("Overpass search failed for {}: {}", cat, e.getMessage());
            }
            if (all.size() >= 30) break; // Enough places, stop early
        }

        // Fallback if Overpass returned nothing
        if (all.isEmpty()) {
            log.warn("No real places found via Overpass, using fallback");
            all = generateFallbackPlaces(waypoints, categories);
        }

        log.info("Places discovered: {}", all.size());
        return all;
    }


    private List<String> resolveCategories(List<String> interests, String travelStyle) {
        Set<String> cats = new LinkedHashSet<>();

        // Always include food and emergency as baseline
        cats.add("food");

        if (travelStyle != null) {
            switch (travelStyle.toLowerCase()) {
                case "adventure":    cats.add("adventure"); cats.add("nature"); break;
                case "heritage":     cats.add("heritage"); cats.add("attraction"); break;
                case "foodie":       cats.add("food"); cats.add("restaurant"); break;
                case "nature":       cats.add("nature"); cats.add("attraction"); break;
                case "spiritual":    cats.add("spiritual"); cats.add("heritage"); break;
                case "shopping":     cats.add("shopping"); break;
                case "luxury":       cats.add("hotel"); cats.add("attraction"); break;
                case "budget":       cats.add("food"); cats.add("nature"); break;
                case "family":       cats.add("attraction"); cats.add("nature"); break;
                case "romantic":     cats.add("attraction"); cats.add("nature"); break;
                default:             cats.add("attraction"); break;
            }
        }

        if (interests != null) {
            for (String interest : interests) {
                switch (interest.toLowerCase()) {
                    case "food":     cats.add("food"); break;
                    case "temple":   cats.add("spiritual"); break;
                    case "nature":   cats.add("nature"); break;
                    case "museum":   cats.add("heritage"); break;
                    case "shopping": cats.add("shopping"); break;
                    case "rest":     cats.add("hotel"); break;
                    default:         cats.add("attraction"); break;
                }
            }
        }

        // Add hotel if overnight trip (time > 8 hours)
        cats.add("hotel");

        return new ArrayList<>(cats);
    }

    private List<PlaceDTO> generateFallbackPlaces(List<double[]> waypoints, List<String> categories) {
        List<PlaceDTO> places = new ArrayList<>();
        String[] catNames = {"Scenic Viewpoint", "Local Dhaba", "Heritage Temple", "Rest Area Cafe",
                "Nature Park", "Budget Hotel", "Market Area", "Tourist Attraction"};
        String[] catTypes = {"nature", "food", "spiritual", "food", "nature", "hotel", "shopping", "attraction"};

        Random r = new Random(waypoints.hashCode());
        for (int i = 0; i < Math.min(catNames.length, 6); i++) {
            double[] wp = waypoints.get(r.nextInt(waypoints.size()));
            PlaceDTO p = new PlaceDTO();
            p.setName(catNames[i]);
            p.setCategory(catTypes[i]);
            p.setLatitude(wp[0] + (r.nextDouble() - 0.5) * 0.05);
            p.setLongitude(wp[1] + (r.nextDouble() - 0.5) * 0.05);
            p.setAddress("Near route");
            p.setRating(3.5 + r.nextDouble() * 1.3);
            p.setEstimatedCost(getCostForCategory(catTypes[i]));
            p.setOpenNow(true);
            places.add(p);
        }
        return places;
    }

    private double getCostForCategory(String cat) {
        switch (cat) {
            case "food": return 150 + Math.random() * 250;
            case "hotel": return 800 + Math.random() * 1500;
            case "attraction": case "heritage": return 50 + Math.random() * 150;
            case "nature": case "spiritual": return 0;
            case "shopping": return 300 + Math.random() * 500;
            default: return 100;
        }
    }

    private void setDistancesFromRoute(List<PlaceDTO> places, RouteDTO route) {
        if (route.getWaypoints() == null || route.getWaypoints().isEmpty()) return;
        for (PlaceDTO p : places) {
            if (p.getDistanceFromRoute() != null) continue;
            double minDist = Double.MAX_VALUE;
            for (double[] wp : route.getWaypoints()) {
                double d = haversine(wp[0], wp[1], p.getLatitude(), p.getLongitude());
                if (d < minDist) minDist = d;
            }
            p.setDistanceFromRoute(round2(minDist));
        }
    }

    private List<PlaceDTO> removeDuplicates(List<PlaceDTO> places) {
        Map<String, PlaceDTO> unique = new LinkedHashMap<>();
        for (PlaceDTO p : places) {
            if (p.getName() == null || p.getName().isBlank()) continue;
            String key = p.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            if (!unique.containsKey(key)) unique.put(key, p);
        }
        return new ArrayList<>(unique.values());
    }

    private int calculateMaxStops(Double timeAvailable) {
        if (timeAvailable == null) return 4;
        if (timeAvailable <= 2) return 2;
        if (timeAvailable <= 4) return 3;
        if (timeAvailable <= 6) return 4;
        if (timeAvailable <= 10) return 5;
        return 7;
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
        for (RemovedPlaceDTO rm : removed) {
            removedRepo.save(new RemovedPlace(trip.getId(), rm.getName(), rm.getReasonRemoved()));
        }
        SavedItinerary saved = new SavedItinerary();
        saved.setTripId(trip.getId()); saved.setUserId(user.getId()); saved.setAiSummary(aiSummary);
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
        resp.setTripId(trip.getId()); resp.setStartLocation(trip.getStartLocation());
        resp.setDestination(trip.getDestination());
        resp.setSuggestedPlaces(sps.stream().map(this::toPlaceDTO).collect(Collectors.toList()));
        resp.setRemovedPlaces(rps.stream().map(r -> new RemovedPlaceDTO(r.getName(), r.getReasonRemoved())).collect(Collectors.toList()));
        resp.setAiSummary(trip.getAiSummary());
        resp.setEstimatedTotalCost(trip.getEstimatedTotalCost() != null ? trip.getEstimatedTotalCost() : 0.0);
        resp.setEstimatedTravelTime(trip.getEstimatedTravelTime());
        resp.setRouteSummary(trip.getRouteSummary());
        resp.setWeatherSummary(trip.getWeatherSummary());
        resp.setDemoMode(false); resp.setFallback(false);
        return resp;
    }

    /** Fetch additional places for "Show More" feature */
    public List<PlaceDTO> getMorePlaces(String tripId, String category, User user, int offset) {
        Trip trip = tripRepo.findById(tripId).orElseThrow(() -> new RuntimeException("Trip not found"));
        if (!trip.getUserId().equals(user.getId())) throw new RuntimeException("Unauthorized");

        // Geocode destination to get coordinates for search
        double[] coords = nominatimService.geocode(trip.getDestination());
        List<PlaceDTO> places = overpassApiService.searchNearby(category, coords[0], coords[1], 5000);

        // Skip already-saved places
        List<SuggestedPlace> existing = placeRepo.findByTripIdOrderByStopOrderAsc(tripId);
        Set<String> existingNames = existing.stream()
            .map(p -> p.getName().toLowerCase().replaceAll("[^a-z0-9]", ""))
            .collect(Collectors.toSet());

        return places.stream()
            .filter(p -> !existingNames.contains(p.getName().toLowerCase().replaceAll("[^a-z0-9]", "")))
            .skip(offset).limit(5)
            .collect(Collectors.toList());
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
        saved.setTripId(trip.getId()); saved.setUserId(user.getId());
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

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
