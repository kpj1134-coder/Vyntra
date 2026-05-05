package com.vyntra.controller;

import com.vyntra.dto.PlaceDTO;
import com.vyntra.service.SerpApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceController {
    private final SerpApiService serpApiService;
    public PlaceController(SerpApiService serpApiService) { this.serpApiService = serpApiService; }

    @GetMapping("/search")
    public ResponseEntity<List<PlaceDTO>> searchPlaces(@RequestParam String query,
            @RequestParam double lat, @RequestParam double lng) {
        return ResponseEntity.ok(serpApiService.searchPlaces(query, lat, lng));
    }
}
