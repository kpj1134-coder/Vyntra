package com.vyntra.dto;

import java.util.List;

public class TripPlanRequest {

    private String startLocation;
    private String destination;
    private Double budget;
    private Double timeAvailable;
    private String energyLevel;
    private String travelType;
    private String mode;
    private List<String> interests;
    private String problemFaced;

    public TripPlanRequest() {}

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

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public String getProblemFaced() { return problemFaced; }
    public void setProblemFaced(String problemFaced) { this.problemFaced = problemFaced; }
}
