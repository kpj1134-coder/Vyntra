package com.vyntra.dto;

import java.time.LocalDateTime;

public class TripHistoryDTO {
    private String id;
    private String startLocation;
    private String destination;
    private Double budget;
    private Double timeAvailable;
    private String energyLevel;
    private String travelType;
    private String mode;
    private int placesCount;
    private LocalDateTime createdAt;
    private boolean hasSavedItinerary;

    public TripHistoryDTO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public Double getTimeAvailable() { return timeAvailable; }
    public void setTimeAvailable(Double timeAvailable) { this.timeAvailable = timeAvailable; }

    public String getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(String energyLevel) { this.energyLevel = energyLevel; }

    public String getTravelType() { return travelType; }
    public void setTravelType(String travelType) { this.travelType = travelType; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public int getPlacesCount() { return placesCount; }
    public void setPlacesCount(int placesCount) { this.placesCount = placesCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isHasSavedItinerary() { return hasSavedItinerary; }
    public void setHasSavedItinerary(boolean hasSavedItinerary) { this.hasSavedItinerary = hasSavedItinerary; }
}
