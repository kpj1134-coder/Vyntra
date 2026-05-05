package com.vyntra.controller;

import com.vyntra.dto.WeatherDTO;
import com.vyntra.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherService weatherService;
    public WeatherController(WeatherService weatherService) { this.weatherService = weatherService; }

    @GetMapping
    public ResponseEntity<WeatherDTO> getWeather(@RequestParam String location) {
        return ResponseEntity.ok(weatherService.fetchWeather(location));
    }
}
