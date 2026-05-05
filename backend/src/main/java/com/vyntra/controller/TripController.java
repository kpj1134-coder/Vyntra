package com.vyntra.controller;

import com.vyntra.dto.*;
import com.vyntra.model.User;
import com.vyntra.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final TripService tripService;
    public TripController(TripService tripService) { this.tripService = tripService; }

    @PostMapping("/plan")
    public ResponseEntity<TripPlanResponse> planTrip(@RequestBody TripPlanRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.planTrip(request, user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TripHistoryDTO>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.getTripHistory(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripPlanResponse> getTrip(@PathVariable String id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.getTripById(id, user));
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Map<String, String>> saveItinerary(@PathVariable String id,
            @AuthenticationPrincipal User user) {
        tripService.saveItinerary(id, user);
        return ResponseEntity.ok(Map.of("message", "Itinerary saved successfully"));
    }
}
