package com.vyntra.dto;

import java.util.List;

/** EmotionRoute AI Mood Planner - Full response to frontend */
public class MoodPlanResponse {

    private MoodAnalysis moodAnalysis;
    private List<String> recommendedPlaces;
    private String tripAdvice;
    private boolean fallback;             // true if Gemini failed
    private String fallbackMessage;

    public MoodPlanResponse() {}

    public MoodAnalysis getMoodAnalysis() { return moodAnalysis; }
    public void setMoodAnalysis(MoodAnalysis moodAnalysis) { this.moodAnalysis = moodAnalysis; }

    public List<String> getRecommendedPlaces() { return recommendedPlaces; }
    public void setRecommendedPlaces(List<String> recommendedPlaces) { this.recommendedPlaces = recommendedPlaces; }

    public String getTripAdvice() { return tripAdvice; }
    public void setTripAdvice(String tripAdvice) { this.tripAdvice = tripAdvice; }

    public boolean isFallback() { return fallback; }
    public void setFallback(boolean fallback) { this.fallback = fallback; }

    public String getFallbackMessage() { return fallbackMessage; }
    public void setFallbackMessage(String fallbackMessage) { this.fallbackMessage = fallbackMessage; }
}
