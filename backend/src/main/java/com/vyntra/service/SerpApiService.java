package com.vyntra.service;

import com.vyntra.dto.PlaceDTO;
import com.vyntra.dto.RouteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class SerpApiService {

    private static final Logger log = LoggerFactory.getLogger(SerpApiService.class);

    private final WebClient webClient;

    @Value("${serpapi.key}")
    private String serpApiKey;

    public SerpApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    public double[] getCoordinates(String location) {
        if ("demo".equals(serpApiKey) || location == null || location.trim().isEmpty()) {
            return generateFallbackCoordinates(location);
        }

        try {
            String url = String.format(
                "https://serpapi.com/search.json?engine=google_maps&type=place&q=%s&api_key=%s",
                encodeParam(location), serpApiKey
            );

            Map response = webClient.get().uri(url).retrieve().bodyToMono(Map.class).block();
            
            if (response != null && response.containsKey("place_results")) {
                Map placeResults = (Map) response.get("place_results");
                if (placeResults.containsKey("gps_coordinates")) {
                    Map coords = (Map) placeResults.get("gps_coordinates");
                    return new double[]{
                        ((Number) coords.get("latitude")).doubleValue(),
                        ((Number) coords.get("longitude")).doubleValue()
                    };
                }
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for {}: {}", location, e.getMessage());
        }
        return generateFallbackCoordinates(location);
    }

    public RouteDTO fetchDirections(String start, String destination) {
        if (!"demo".equals(serpApiKey)) {
            try {
                String url = String.format(
                    "https://serpapi.com/search.json?engine=google_maps_directions&start=%s&end=%s&api_key=%s",
                    encodeParam(start), encodeParam(destination), serpApiKey
                );

                Map response = webClient.get().uri(url).retrieve().bodyToMono(Map.class).block();
                RouteDTO route = parseDirectionsResponse(response, start, destination);
                if (route != null && route.getWaypoints() != null && !route.getWaypoints().isEmpty()) {
                    return route;
                }
            } catch (Exception e) {
                log.warn("Directions API failed, using fallback route generation: {}", e.getMessage());
            }
        }

        // Fallback: Generate a straight-line route with intermediate points
        return generateFallbackRoute(start, destination);
    }

    public List<PlaceDTO> searchPlaces(String query, double latitude, double longitude) {
        if ("demo".equals(serpApiKey)) {
            return generateGenericDemoPlaces(query, latitude, longitude);
        }

        try {
            String url = String.format(
                "https://serpapi.com/search.json?engine=google_maps&q=%s&ll=@%f,%f,15z&type=search&api_key=%s",
                encodeParam(query), latitude, longitude, serpApiKey
            );

            Map response = webClient.get().uri(url).retrieve().bodyToMono(Map.class).block();
            return parsePlacesResponse(response, query, latitude, longitude);
        } catch (Exception e) {
            log.warn("Places search failed, using generic mock data: {}", e.getMessage());
            return generateGenericDemoPlaces(query, latitude, longitude);
        }
    }

    private RouteDTO parseDirectionsResponse(Map response, String start, String destination) {
        RouteDTO route = new RouteDTO();
        try {
            if (response != null && response.containsKey("directions")) {
                List<Map> directions = (List<Map>) response.get("directions");
                if (!directions.isEmpty()) {
                    Map firstRoute = directions.get(0);
                    String distance = (String) firstRoute.getOrDefault("distance", "20 km");
                    String duration = (String) firstRoute.getOrDefault("duration", "45 mins");

                    double distKm = parseDistance(distance);
                    route.setTotalDistanceKm(distKm);
                    route.setTotalDuration(duration);

                    List<double[]> waypoints = new ArrayList<>();
                    if (firstRoute.containsKey("legs")) {
                        List<Map> legs = (List<Map>) firstRoute.get("legs");
                        for (Map leg : legs) {
                            if (leg.containsKey("steps")) {
                                List<Map> steps = (List<Map>) leg.get("steps");
                                for (Map step : steps) {
                                    if (step.containsKey("start_location")) {
                                        Map loc = (Map) step.get("start_location");
                                        double lat = ((Number) loc.get("lat")).doubleValue();
                                        double lng = ((Number) loc.get("lng")).doubleValue();
                                        waypoints.add(new double[]{lat, lng});
                                    }
                                }
                            }
                        }
                    }
                    route.setWaypoints(waypoints);
                    return route;
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing directions: {}", e.getMessage());
        }
        return null;
    }

    private List<PlaceDTO> parsePlacesResponse(Map response, String query, double lat, double lng) {
        List<PlaceDTO> places = new ArrayList<>();
        try {
            if (response != null && response.containsKey("local_results")) {
                List<Map> results = (List<Map>) response.get("local_results");
                for (Map result : results) {
                    PlaceDTO place = new PlaceDTO();
                    place.setName((String) result.getOrDefault("title", "Unknown Place"));
                    place.setAddress((String) result.getOrDefault("address", ""));
                    place.setCategory(categorizeFromQuery(query));

                    Object ratingObj = result.get("rating");
                    place.setRating(ratingObj != null ? ((Number) ratingObj).doubleValue() : 3.5 + Math.random());

                    if (result.containsKey("gps_coordinates")) {
                        Map coords = (Map) result.get("gps_coordinates");
                        place.setLatitude(((Number) coords.get("latitude")).doubleValue());
                        place.setLongitude(((Number) coords.get("longitude")).doubleValue());
                    } else {
                        place.setLatitude(lat + (Math.random() - 0.5) * 0.02);
                        place.setLongitude(lng + (Math.random() - 0.5) * 0.02);
                    }

                    Object hoursObj = result.get("hours");
                    place.setOpenNow(hoursObj != null ? hoursObj.toString().toLowerCase().contains("open") : true);

                    place.setEstimatedCost(estimateCost(query));
                    places.add(place);
                }
            }
        } catch (Exception e) {
            log.warn("Error parsing places: {}", e.getMessage());
        }
        return places.isEmpty() ? generateGenericDemoPlaces(query, lat, lng) : places;
    }

    // ====== DYNAMIC FALLBACKS FOR ANY LOCATION ======

    private double[] generateFallbackCoordinates(String location) {
        if (location == null) return new double[]{12.9716, 77.5946}; // Bangalore default
        String loc = location.toLowerCase();
        if (loc.contains("chennai")) return new double[]{13.0827, 80.2707};
        if (loc.contains("bangalore") || loc.contains("bengaluru")) return new double[]{12.9716, 77.5946};
        if (loc.contains("delhi")) return new double[]{28.7041, 77.1025};
        if (loc.contains("mumbai")) return new double[]{19.0760, 72.8777};
        if (loc.contains("hyderabad")) return new double[]{17.3850, 78.4867};
        if (loc.contains("pune")) return new double[]{18.5204, 73.8567};
        if (loc.contains("mysore")) return new double[]{12.2958, 76.6394};
        if (loc.contains("goa")) return new double[]{15.2993, 74.1240};
        if (loc.contains("agra")) return new double[]{27.1767, 78.0081};
        
        // Pseudo-random coordinates based on hash code to be deterministic
        Random r = new Random(location.hashCode());
        double lat = 10.0 + (r.nextDouble() * 20.0); // India approx
        double lng = 70.0 + (r.nextDouble() * 15.0);
        return new double[]{lat, lng};
    }

    private RouteDTO generateFallbackRoute(String start, String destination) {
        double[] startCoords = getCoordinates(start);
        double[] endCoords = getCoordinates(destination);
        
        double distance = haversineDistance(startCoords[0], startCoords[1], endCoords[0], endCoords[1]);
        
        RouteDTO route = new RouteDTO();
        route.setTotalDistanceKm(Math.round(distance * 1.2 * 100.0) / 100.0); // +20% for road distance
        int minutes = (int) ((distance * 1.2) / 60.0 * 60); // 60 km/h avg speed
        int hours = minutes / 60;
        route.setTotalDuration((hours > 0 ? hours + " hours " : "") + (minutes % 60) + " mins");
        
        List<double[]> waypoints = new ArrayList<>();
        waypoints.add(startCoords);
        
        // Create 3 intermediate points
        int numPoints = 3;
        for (int i = 1; i <= numPoints; i++) {
            double fraction = (double) i / (numPoints + 1);
            double midLat = startCoords[0] + (endCoords[0] - startCoords[0]) * fraction;
            double midLng = startCoords[1] + (endCoords[1] - startCoords[1]) * fraction;
            
            // Add slight random deviation so it's not perfectly straight
            Random r = new Random((start + destination + i).hashCode());
            midLat += (r.nextDouble() - 0.5) * 0.05;
            midLng += (r.nextDouble() - 0.5) * 0.05;
            
            waypoints.add(new double[]{midLat, midLng});
        }
        
        waypoints.add(endCoords);
        route.setWaypoints(waypoints);
        
        return route;
    }

    private List<PlaceDTO> generateGenericDemoPlaces(String query, double lat, double lng) {
        List<PlaceDTO> places = new ArrayList<>();
        String category = categorizeFromQuery(query);
        Random r = new Random(Double.doubleToLongBits(lat) ^ Double.doubleToLongBits(lng) ^ query.hashCode());

        String[] prefixes = {"The Grand", "Royal", "New", "Local", "Famous", "Hidden", "Premium", "Classic"};
        String[] suffixes = getSuffixesForCategory(category);
        
        for (int i = 0; i < 3; i++) {
            PlaceDTO p = new PlaceDTO();
            String name = prefixes[r.nextInt(prefixes.length)] + " " + suffixes[r.nextInt(suffixes.length)];
            p.setName(name);
            p.setCategory(category);
            p.setAddress("Near Route Point");
            p.setRating(3.5 + r.nextDouble() * 1.4);
            p.setEstimatedCost(estimateCost(category) * (0.8 + r.nextDouble() * 0.4));
            
            p.setLatitude(lat + (r.nextDouble() - 0.5) * 0.03); // within ~3km
            p.setLongitude(lng + (r.nextDouble() - 0.5) * 0.03);
            p.setOpenNow(r.nextDouble() > 0.1); // 90% open
            
            double dist = haversineDistance(lat, lng, p.getLatitude(), p.getLongitude());
            p.setDistanceFromRoute(Math.round(dist * 100.0) / 100.0);
            
            places.add(p);
        }
        return places;
    }

    private String[] getSuffixesForCategory(String category) {
        switch (category) {
            case "food": return new String[]{"Diner", "Cafe", "Restaurant", "Eatery", "Bistro", "Kitchen"};
            case "temple": return new String[]{"Temple", "Shrine", "Ashram", "Mandir", "Sanctuary"};
            case "nature": return new String[]{"Park", "Gardens", "Lake", "Reserve", "Hill", "Viewpoint"};
            case "hotel": return new String[]{"Hotel", "Inn", "Resort", "Suites", "Lodge", "Stay"};
            case "rest": return new String[]{"Coffee Shop", "Rest Stop", "Lounge", "Bakery", "Brewery"};
            case "shopping": return new String[]{"Mall", "Market", "Plaza", "Bazaar", "Mart"};
            case "emergency": return new String[]{"Hospital", "Pharmacy", "Clinic", "Station", "ATM"};
            default: return new String[]{"Place", "Spot", "Hub", "Center", "Point"};
        }
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private String categorizeFromQuery(String query) {
        String q = query.toLowerCase();
        if (q.contains("food") || q.contains("restaurant") || q.contains("cafe") || q.contains("eat")) return "food";
        if (q.contains("temple") || q.contains("church") || q.contains("mosque")) return "temple";
        if (q.contains("nature") || q.contains("park") || q.contains("lake")) return "nature";
        if (q.contains("hotel") || q.contains("stay") || q.contains("lodge")) return "hotel";
        if (q.contains("shop") || q.contains("market")) return "shopping";
        if (q.contains("rest") || q.contains("coffee")) return "rest";
        if (q.contains("atm") || q.contains("fuel") || q.contains("hospital") || q.contains("emergency")) return "emergency";
        return "general";
    }

    private double estimateCost(String query) {
        String q = query.toLowerCase();
        if (q.contains("temple") || q.contains("park") || q.contains("lake")) return 0;
        if (q.contains("food") || q.contains("cafe") || q.contains("restaurant")) return 250;
        if (q.contains("hotel")) return 2000;
        if (q.contains("shopping")) return 500;
        return 100;
    }

    private double parseDistance(String distance) {
        try {
            return Double.parseDouble(distance.replaceAll("[^0-9.]", ""));
        } catch (Exception e) { return 20.0; }
    }

    private String encodeParam(String param) {
        return java.net.URLEncoder.encode(param, java.nio.charset.StandardCharsets.UTF_8);
    }
}
