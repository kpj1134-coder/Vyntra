package com.vyntra.controller;

import com.vyntra.dto.MoodPlanRequest;
import com.vyntra.dto.MoodPlanResponse;
import com.vyntra.service.MoodPlannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * EmotionRoute AI Mood Planner Controller
 * POST /api/ai/mood-plan
 */
@RestController
@RequestMapping("/api/ai")
public class MoodController {

    private final MoodPlannerService moodPlannerService;

    public MoodController(MoodPlannerService moodPlannerService) {
        this.moodPlannerService = moodPlannerService;
    }

    /**
     * Analyze user mood and return structured AI trip recommendations.
     * Existing trip planning, nearby places, and route features are NOT affected.
     */
    @PostMapping("/mood-plan")
    public ResponseEntity<MoodPlanResponse> moodPlan(@RequestBody MoodPlanRequest request) {
        MoodPlanResponse response = moodPlannerService.analyzeMoodAndPlan(request);
        return ResponseEntity.ok(response);
    }
}
