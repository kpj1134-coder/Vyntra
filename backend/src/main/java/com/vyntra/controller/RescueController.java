package com.vyntra.controller;

import com.vyntra.dto.TripPlanResponse;
import com.vyntra.dto.RescueRequest;
import com.vyntra.model.User;
import com.vyntra.service.RescueService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rescue")
public class RescueController {
    private final RescueService rescueService;
    public RescueController(RescueService rescueService) { this.rescueService = rescueService; }

    @PostMapping("/replan")
    public ResponseEntity<TripPlanResponse> replan(@RequestBody RescueRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(rescueService.rescue(request, user));
    }
}
