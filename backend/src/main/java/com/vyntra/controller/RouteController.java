package com.vyntra.controller;

import com.vyntra.dto.RouteDTO;
import com.vyntra.service.SerpApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/route")
public class RouteController {
    private final SerpApiService serpApiService;
    public RouteController(SerpApiService serpApiService) { this.serpApiService = serpApiService; }

    @GetMapping("/directions")
    public ResponseEntity<RouteDTO> getDirections(@RequestParam String start, @RequestParam String destination) {
        return ResponseEntity.ok(serpApiService.fetchDirections(start, destination));
    }
}
