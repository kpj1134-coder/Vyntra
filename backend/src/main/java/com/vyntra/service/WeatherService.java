package com.vyntra.service;

import com.vyntra.dto.WeatherDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import java.util.Random;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private final WebClient webClient;
    @Value("${weather.api.key}") private String weatherApiKey;

    public WeatherService(WebClient webClient) { this.webClient = webClient; }

    public WeatherDTO fetchWeather(String location) {
        if ("demo".equals(weatherApiKey)) return generateDynamicWeather(location);
        try {
            String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric",
                java.net.URLEncoder.encode(location, java.nio.charset.StandardCharsets.UTF_8), weatherApiKey);
            Map response = webClient.get().uri(url).retrieve().bodyToMono(Map.class).block();
            return parseWeatherResponse(response, location);
        } catch (Exception e) {
            log.warn("Weather API failed: {}", e.getMessage());
            return generateDynamicWeather(location);
        }
    }

    private WeatherDTO parseWeatherResponse(Map response, String location) {
        WeatherDTO w = new WeatherDTO();
        w.setLocation(location);
        try {
            if (response == null) return generateDynamicWeather(location);
            Map main = (Map) response.get("main");
            if (main != null) { w.setTemperature(((Number) main.get("temp")).doubleValue()); w.setHumidity(((Number) main.get("humidity")).doubleValue()); }
            Map wind = (Map) response.get("wind");
            if (wind != null) w.setWindSpeed(((Number) wind.get("speed")).doubleValue());
            java.util.List<Map> wl = (java.util.List<Map>) response.get("weather");
            if (wl != null && !wl.isEmpty()) {
                String cond = (String) wl.get(0).get("main");
                w.setCondition((String) wl.get(0).getOrDefault("description", cond));
                w.setIcon((String) wl.get(0).get("icon"));
                w.setRainy(cond != null && (cond.toLowerCase().contains("rain") || cond.toLowerCase().contains("drizzle")));
            }
            return w;
        } catch (Exception e) { return generateDynamicWeather(location); }
    }

    private WeatherDTO generateDynamicWeather(String location) {
        Random r = new Random(location != null ? location.hashCode() : 42);
        WeatherDTO w = new WeatherDTO();
        w.setLocation(location);
        w.setTemperature((double) (20 + r.nextInt(15)));
        w.setHumidity((double) (40 + r.nextInt(40)));
        w.setWindSpeed((double) (5 + r.nextInt(15)));
        
        boolean isRainy = r.nextDouble() > 0.8; // 20% chance of rain
        w.setRainy(isRainy);
        w.setCondition(isRainy ? "Light Rain" : (r.nextBoolean() ? "Clear Sky" : "Partly Cloudy"));
        w.setIcon(isRainy ? "09d" : "02d");
        
        return w;
    }
}
