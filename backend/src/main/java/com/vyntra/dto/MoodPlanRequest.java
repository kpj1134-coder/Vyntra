package com.vyntra.dto;

import java.util.List;

/** EmotionRoute AI Mood Planner - Request DTO */
public class MoodPlanRequest {

    private String source;
    private String destination;
    private String userMoodText;       // Free text: "I am tired and don't want crowded places"
    private String selectedMood;       // Quick button: Relaxing, Adventure, Heritage, etc.
    private List<String> nearbyPlaces; // Optional context

    public MoodPlanRequest() {}

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getUserMoodText() { return userMoodText; }
    public void setUserMoodText(String userMoodText) { this.userMoodText = userMoodText; }

    public String getSelectedMood() { return selectedMood; }
    public void setSelectedMood(String selectedMood) { this.selectedMood = selectedMood; }

    public List<String> getNearbyPlaces() { return nearbyPlaces; }
    public void setNearbyPlaces(List<String> nearbyPlaces) { this.nearbyPlaces = nearbyPlaces; }
}
