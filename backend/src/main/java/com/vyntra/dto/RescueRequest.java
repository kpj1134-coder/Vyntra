package com.vyntra.dto;

import java.util.List;

public class RescueRequest {
    private String currentLocation;
    private String destination;
    private Double remainingBudget;
    private Double remainingTime;
    private String currentEnergy;
    private String issue;
    private List<String> interests;

    public RescueRequest() {}

    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Double getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(Double remainingBudget) { this.remainingBudget = remainingBudget; }

    public Double getRemainingTime() { return remainingTime; }
    public void setRemainingTime(Double remainingTime) { this.remainingTime = remainingTime; }

    public String getCurrentEnergy() { return currentEnergy; }
    public void setCurrentEnergy(String currentEnergy) { this.currentEnergy = currentEnergy; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }
}
