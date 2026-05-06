package com.vyntra.service;

import com.vyntra.dto.PlaceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;

import java.util.*;

@Service
public class OverpassApiService {

    private static final Logger log = LoggerFactory.getLogger(OverpassApiService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OverpassApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    /** Search for real places near a lat/lon using OpenStreetMap Overpass API */
    public List<PlaceDTO> searchNearby(String category, double lat, double lon, int radiusMeters) {
        String query = buildOverpassQuery(category, lat, lon, radiusMeters);
        try {
            String response = webClient.post()
                    .uri("https://overpass-api.de/api/interpreter")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8))
                    .header("User-Agent", "Vyntra-Travel-App/1.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(8))
                    .block();

            return parseOverpassResponse(response, category, lat, lon);
        } catch (Exception e) {
            log.warn("Overpass API failed for category '{}' at [{},{}]: {}", category, lat, lon, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildOverpassQuery(String category, double lat, double lon, int radius) {
        String around = "(around:" + radius + "," + lat + "," + lon + ")";
        switch (category.toLowerCase()) {
            case "food": case "restaurant":
                return "[out:json][timeout:7];(node[\"amenity\"~\"restaurant|cafe|fast_food\"]" + around + ";);out body 12;";
            case "hotel": case "stay":
                return "[out:json][timeout:7];(node[\"tourism\"~\"hotel|motel|hostel|guest_house\"]" + around + ";);out body 8;";
            case "attraction": case "tourism":
                return "[out:json][timeout:7];(node[\"tourism\"~\"attraction|museum|viewpoint|zoo\"]" + around + ";);out body 12;";
            case "heritage": case "historic":
                return "[out:json][timeout:7];(node[\"historic\"~\"monument|castle|ruins|fort\"]" + around + ";node[\"tourism\"=\"museum\"]" + around + ";);out body 10;";
            case "nature": case "park":
                return "[out:json][timeout:7];(node[\"leisure\"~\"park|nature_reserve|garden\"]" + around + ";node[\"natural\"~\"peak|waterfall|beach\"]" + around + ";);out body 10;";
            case "shopping":
                return "[out:json][timeout:7];(node[\"shop\"~\"mall|supermarket|market\"]" + around + ";);out body 8;";
            case "spiritual": case "temple":
                return "[out:json][timeout:7];(node[\"amenity\"=\"place_of_worship\"]" + around + ";);out body 10;";
            case "adventure":
                return "[out:json][timeout:7];(node[\"leisure\"~\"sports_centre|climbing\"]" + around + ";node[\"tourism\"=\"viewpoint\"]" + around + ";);out body 8;";
            case "emergency": case "hospital":
                return "[out:json][timeout:7];(node[\"amenity\"~\"hospital|clinic|pharmacy|atm\"]" + around + ";);out body 8;";
            default:
                return "[out:json][timeout:7];(node[\"amenity\"~\"restaurant|cafe\"]" + around + ";node[\"tourism\"]" + around + ";);out body 12;";
        }
    }

    private List<PlaceDTO> parseOverpassResponse(String json, String category, double refLat, double refLon) {
        List<PlaceDTO> places = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode elements = root.path("elements");
            for (JsonNode el : elements) {
                JsonNode tags = el.path("tags");
                String name = tags.path("name").asText("").trim();
                if (name.isEmpty()) continue; // skip unnamed places

                double lat, lon;
                if (el.has("lat")) {
                    lat = el.path("lat").asDouble();
                    lon = el.path("lon").asDouble();
                } else if (el.has("center")) {
                    lat = el.path("center").path("lat").asDouble();
                    lon = el.path("center").path("lon").asDouble();
                } else continue;

                PlaceDTO p = new PlaceDTO();
                p.setName(name);
                p.setLatitude(lat);
                p.setLongitude(lon);
                p.setCategory(mapCategory(tags, category));
                p.setAddress(buildAddress(tags));
                p.setRating(estimateRating(tags));
                p.setEstimatedCost(estimateCost(tags, category));
                p.setOpenNow(estimateOpenNow(tags));
                double dist = haversine(refLat, refLon, lat, lon);
                p.setDistanceFromRoute(Math.round(dist * 100.0) / 100.0);

                places.add(p);
                if (places.size() >= 20) break;
            }
        } catch (Exception e) {
            log.warn("Error parsing Overpass response: {}", e.getMessage());
        }
        return places;
    }

    private String mapCategory(JsonNode tags, String fallback) {
        String amenity = tags.path("amenity").asText("");
        String tourism = tags.path("tourism").asText("");
        String historic = tags.path("historic").asText("");
        String leisure = tags.path("leisure").asText("");
        String natural = tags.path("natural").asText("");
        if (amenity.matches("restaurant|cafe|fast_food|food_court")) return "food";
        if (amenity.matches("hospital|clinic|pharmacy|police")) return "emergency";
        if (amenity.equals("place_of_worship")) return "spiritual";
        if (amenity.matches("atm|bank")) return "emergency";
        if (tourism.matches("hotel|motel|hostel|guest_house")) return "hotel";
        if (tourism.matches("museum|attraction|viewpoint|zoo")) return "attraction";
        if (!historic.isEmpty()) return "heritage";
        if (leisure.matches("park|nature_reserve|garden")) return "nature";
        if (!natural.isEmpty()) return "nature";
        if (leisure.matches("sports_centre|climbing")) return "adventure";
        return fallback.isEmpty() ? "general" : fallback;
    }

    private String buildAddress(JsonNode tags) {
        String street = tags.path("addr:street").asText("");
        String city = tags.path("addr:city").asText("");
        String state = tags.path("addr:state").asText("");
        StringBuilder sb = new StringBuilder();
        if (!street.isEmpty()) sb.append(street);
        if (!city.isEmpty()) { if (sb.length() > 0) sb.append(", "); sb.append(city); }
        if (!state.isEmpty()) { if (sb.length() > 0) sb.append(", "); sb.append(state); }
        return sb.length() > 0 ? sb.toString() : "Near your route";
    }

    private double estimateRating(JsonNode tags) {
        // OSM doesn't have ratings; estimate from number of tags as proxy for popularity
        int tagCount = tags.size();
        if (tagCount > 8) return 4.2 + Math.random() * 0.6;
        if (tagCount > 4) return 3.7 + Math.random() * 0.8;
        return 3.2 + Math.random() * 1.0;
    }

    private double estimateCost(JsonNode tags, String category) {
        String feeStr = tags.path("fee").asText("");
        if (feeStr.equals("no")) return 0;
        switch (category.toLowerCase()) {
            case "food": case "restaurant": return 150 + Math.random() * 300;
            case "hotel": case "stay": return 800 + Math.random() * 2000;
            case "attraction": case "tourism": return 50 + Math.random() * 200;
            case "heritage": return 30 + Math.random() * 150;
            case "nature": return 0;
            case "shopping": return 200 + Math.random() * 500;
            default: return 50 + Math.random() * 100;
        }
    }

    private boolean estimateOpenNow(JsonNode tags) {
        String hours = tags.path("opening_hours").asText("");
        if (hours.isEmpty()) return true;
        return !hours.toLowerCase().contains("closed");
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
