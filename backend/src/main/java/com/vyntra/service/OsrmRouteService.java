package com.vyntra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyntra.dto.RouteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class OsrmRouteService {

    private static final Logger log = LoggerFactory.getLogger(OsrmRouteService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OsrmRouteService(WebClient webClient) {
        this.webClient = webClient;
    }

    /** Get real driving route using free OSRM (no API key needed) */
    public RouteDTO getRoute(double startLat, double startLon, double endLat, double endLon) {
        try {
            // OSRM format: lon,lat (note: longitude first!)
            String url = String.format(
                "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson&steps=false",
                startLon, startLat, endLon, endLat
            );

            String response = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Vyntra-Travel-App/1.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseOsrmResponse(response, startLat, startLon, endLat, endLon);
        } catch (Exception e) {
            log.warn("OSRM routing failed, using straight-line: {}", e.getMessage());
            return buildStraightLineRoute(startLat, startLon, endLat, endLon);
        }
    }

    private RouteDTO parseOsrmResponse(String json, double sLat, double sLon, double eLat, double eLon) {
        RouteDTO route = new RouteDTO();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!"Ok".equals(root.path("code").asText())) {
                return buildStraightLineRoute(sLat, sLon, eLat, eLon);
            }
            JsonNode routeNode = root.path("routes").get(0);
            double distanceMeters = routeNode.path("distance").asDouble();
            double durationSeconds = routeNode.path("duration").asDouble();

            double distKm = Math.round(distanceMeters / 100.0) / 10.0;
            int totalMinutes = (int)(durationSeconds / 60);
            int hours = totalMinutes / 60;
            int mins = totalMinutes % 60;
            String duration = (hours > 0 ? hours + " hr " : "") + mins + " min";

            route.setTotalDistanceKm(distKm);
            route.setTotalDuration(duration);

            // Extract route waypoints from GeoJSON geometry
            List<double[]> waypoints = new ArrayList<>();
            JsonNode geometry = routeNode.path("geometry").path("coordinates");
            if (geometry.isArray()) {
                int total = geometry.size();
                int step = Math.max(1, total / 6); // max 6 checkpoints
                for (int i = 0; i < total; i += step) {
                    JsonNode coord = geometry.get(i);
                    double lon = coord.get(0).asDouble();
                    double lat = coord.get(1).asDouble();
                    waypoints.add(new double[]{lat, lon});
                }
                // Always add end point
                JsonNode last = geometry.get(total - 1);
                waypoints.add(new double[]{last.get(1).asDouble(), last.get(0).asDouble()});
            }
            if (waypoints.isEmpty()) {
                waypoints.add(new double[]{sLat, sLon});
                waypoints.add(new double[]{eLat, eLon});
            }
            route.setWaypoints(waypoints);
            log.info("OSRM route: {} km, {}, {} waypoints", distKm, duration, waypoints.size());
            return route;
        } catch (Exception e) {
            log.warn("Error parsing OSRM response: {}", e.getMessage());
            return buildStraightLineRoute(sLat, sLon, eLat, eLon);
        }
    }

    private RouteDTO buildStraightLineRoute(double sLat, double sLon, double eLat, double eLon) {
        double dist = haversine(sLat, sLon, eLat, eLon) * 1.3; // road factor
        int mins = (int)(dist / 60.0 * 60); // 60 km/h avg
        RouteDTO route = new RouteDTO();
        route.setTotalDistanceKm(Math.round(dist * 10.0) / 10.0);
        int h = mins / 60, m = mins % 60;
        route.setTotalDuration((h > 0 ? h + " hr " : "") + m + " min");
        List<double[]> wps = new ArrayList<>();
        wps.add(new double[]{sLat, sLon});
        // 3 intermediate points
        for (int i = 1; i <= 3; i++) {
            double f = i / 4.0;
            wps.add(new double[]{sLat + (eLat - sLat) * f, sLon + (eLon - sLon) * f});
        }
        wps.add(new double[]{eLat, eLon});
        route.setWaypoints(wps);
        return route;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
