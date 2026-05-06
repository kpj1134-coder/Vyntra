package com.vyntra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class NominatimService {

    private static final Logger log = LoggerFactory.getLogger(NominatimService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NominatimService(WebClient webClient) {
        this.webClient = webClient;
    }

    /** Returns [lat, lon] for any location string. Falls back to hash-based coords if fails. */
    public double[] geocode(String location) {
        if (location == null || location.isBlank()) return new double[]{12.9716, 77.5946};
        try {
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + java.net.URLEncoder.encode(location, java.nio.charset.StandardCharsets.UTF_8)
                    + "&format=json&limit=1&addressdetails=0";

            String response = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Vyntra-Travel-App/1.0 (contact@vyntra.app)")
                    .header("Accept", "application/json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                double lat = first.path("lat").asDouble();
                double lon = first.path("lon").asDouble();
                log.info("Geocoded '{}' -> [{}, {}]", location, lat, lon);
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            log.warn("Nominatim geocoding failed for '{}': {}", location, e.getMessage());
        }
        return fallbackCoords(location);
    }

    private double[] fallbackCoords(String loc) {
        if (loc == null) return new double[]{20.5937, 78.9629};
        String l = loc.toLowerCase();
        if (l.contains("chennai")) return new double[]{13.0827, 80.2707};
        if (l.contains("bangalore") || l.contains("bengaluru")) return new double[]{12.9716, 77.5946};
        if (l.contains("delhi")) return new double[]{28.7041, 77.1025};
        if (l.contains("mumbai")) return new double[]{19.0760, 72.8777};
        if (l.contains("hyderabad")) return new double[]{17.3850, 78.4867};
        if (l.contains("pune")) return new double[]{18.5204, 73.8567};
        if (l.contains("mysore") || l.contains("mysuru")) return new double[]{12.2958, 76.6394};
        if (l.contains("goa")) return new double[]{15.2993, 74.1240};
        if (l.contains("agra")) return new double[]{27.1767, 78.0081};
        if (l.contains("jaipur")) return new double[]{26.9124, 75.7873};
        if (l.contains("kolkata")) return new double[]{22.5726, 88.3639};
        if (l.contains("kochi") || l.contains("cochin")) return new double[]{9.9312, 76.2673};
        if (l.contains("paris")) return new double[]{48.8566, 2.3522};
        if (l.contains("london")) return new double[]{51.5074, -0.1278};
        if (l.contains("tokyo")) return new double[]{35.6762, 139.6503};
        if (l.contains("new york")) return new double[]{40.7128, -74.0060};
        java.util.Random r = new java.util.Random(loc.hashCode());
        return new double[]{10.0 + r.nextDouble() * 20.0, 70.0 + r.nextDouble() * 15.0};
    }
}
