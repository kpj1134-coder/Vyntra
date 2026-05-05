package com.vyntra.dto;

import java.util.List;

public class TripPlanResponse {

    private String tripId;
    private String startLocation;
    private String destination;
    private String routeSummary;
    private String weatherSummary;
    private List<PlaceDTO> suggestedPlaces;
    private List<RemovedPlaceDTO> removedPlaces;
    private String aiSummary;
    private Double estimatedTotalCost;
    private String estimatedTravelTime;
    private WeatherDTO weather;
    private RouteDTO route;
    private boolean demoMode;
    private boolean fallback;

    public TripPlanResponse() {}

    // Getters and Setters
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getRouteSummary() { return routeSummary; }
    public void setRouteSummary(String routeSummary) { this.routeSummary = routeSummary; }

    public String getWeatherSummary() { return weatherSummary; }
    public void setWeatherSummary(String weatherSummary) { this.weatherSummary = weatherSummary; }

    public List<PlaceDTO> getSuggestedPlaces() { return suggestedPlaces; }
    public void setSuggestedPlaces(List<PlaceDTO> suggestedPlaces) { this.suggestedPlaces = suggestedPlaces; }

    public List<RemovedPlaceDTO> getRemovedPlaces() { return removedPlaces; }
    public void setRemovedPlaces(List<RemovedPlaceDTO> removedPlaces) { this.removedPlaces = removedPlaces; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public Double getEstimatedTotalCost() { return estimatedTotalCost; }
    public void setEstimatedTotalCost(Double estimatedTotalCost) { this.estimatedTotalCost = estimatedTotalCost; }

    public String getEstimatedTravelTime() { return estimatedTravelTime; }
    public void setEstimatedTravelTime(String estimatedTravelTime) { this.estimatedTravelTime = estimatedTravelTime; }

    public WeatherDTO getWeather() { return weather; }
    public void setWeather(WeatherDTO weather) { this.weather = weather; }

    public RouteDTO getRoute() { return route; }
    public void setRoute(RouteDTO route) { this.route = route; }

    public boolean isDemoMode() { return demoMode; }
    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }

    public boolean isFallback() { return fallback; }
    public void setFallback(boolean fallback) { this.fallback = fallback; }
}
