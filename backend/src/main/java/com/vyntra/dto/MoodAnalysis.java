package com.vyntra.dto;

import java.util.List;

/** EmotionRoute AI Mood Planner - Gemini-parsed mood analysis */
public class MoodAnalysis {

    private String mood;                 // e.g. "tired", "excited", "curious"
    private String energyLevel;          // "low", "medium", "high"
    private String tripType;             // "calm", "adventure", "heritage", "food", "nature"
    private List<String> avoid;          // ["crowded places", "long walking"]
    private List<String> preferredPlaces;// ["parks", "lakes", "quiet cafes"]
    private String foodPreference;       // "light and budget friendly"
    private String routeStyle;           // "short and comfortable"
    private String suggestionReason;     // Human-readable AI explanation

    public MoodAnalysis() {}

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(String energyLevel) { this.energyLevel = energyLevel; }

    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }

    public List<String> getAvoid() { return avoid; }
    public void setAvoid(List<String> avoid) { this.avoid = avoid; }

    public List<String> getPreferredPlaces() { return preferredPlaces; }
    public void setPreferredPlaces(List<String> preferredPlaces) { this.preferredPlaces = preferredPlaces; }

    public String getFoodPreference() { return foodPreference; }
    public void setFoodPreference(String foodPreference) { this.foodPreference = foodPreference; }

    public String getRouteStyle() { return routeStyle; }
    public void setRouteStyle(String routeStyle) { this.routeStyle = routeStyle; }

    public String getSuggestionReason() { return suggestionReason; }
    public void setSuggestionReason(String suggestionReason) { this.suggestionReason = suggestionReason; }
}
