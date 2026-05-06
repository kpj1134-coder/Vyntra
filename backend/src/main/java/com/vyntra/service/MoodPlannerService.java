package com.vyntra.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vyntra.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * EmotionRoute AI Mood Planner Service
 * Uses Gemini AI to classify travel mood and return structured recommendations.
 * Falls back gracefully if Gemini is unavailable.
 */
@Service
public class MoodPlannerService {

    private static final Logger log = LoggerFactory.getLogger(MoodPlannerService.class);

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public MoodPlannerService(GeminiService geminiService, ObjectMapper objectMapper) {
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    public MoodPlanResponse analyzeMoodAndPlan(MoodPlanRequest request) {
        String moodInput = buildMoodInput(request);
        log.info("EmotionRoute: Analyzing mood for '{}'", moodInput);

        try {
            // 1. Ask Gemini to classify mood and return strict JSON
            String geminiPrompt = buildGeminiPrompt(request, moodInput);
            String rawResponse = geminiService.generate(geminiPrompt);

            // 2. Extract JSON from response (Gemini sometimes wraps it in markdown)
            String cleanJson = extractJson(rawResponse);

            // 3. Parse JSON into MoodAnalysis
            MoodAnalysis analysis = parseGeminiJson(cleanJson);

            // 4. Build recommended places from analysis
            List<String> recommendedPlaces = buildRecommendedPlaces(analysis);

            // 5. Build human-readable trip advice
            String advice = buildTripAdvice(analysis, request);

            MoodPlanResponse response = new MoodPlanResponse();
            response.setMoodAnalysis(analysis);
            response.setRecommendedPlaces(recommendedPlaces);
            response.setTripAdvice(advice);
            response.setFallback(false);
            return response;

        } catch (Exception e) {
            log.warn("EmotionRoute: Gemini mood analysis failed, using fallback. Error: {}", e.getMessage());
            return buildFallbackResponse(request);
        }
    }

    /** Build combined mood text from selected button + free text */
    private String buildMoodInput(MoodPlanRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.getSelectedMood() != null && !req.getSelectedMood().isBlank())
            sb.append(req.getSelectedMood()).append(". ");
        if (req.getUserMoodText() != null && !req.getUserMoodText().isBlank())
            sb.append(req.getUserMoodText());
        return sb.toString().trim().isEmpty() ? "General travel" : sb.toString().trim();
    }

    /** Craft a precise Gemini prompt that forces clean JSON output */
    private String buildGeminiPrompt(MoodPlanRequest req, String moodInput) {
        return "You are a travel mood AI assistant. Analyze the user's travel mood and classify it.\n\n"
            + "User mood input: \"" + moodInput + "\"\n"
            + "Trip: " + req.getSource() + " to " + req.getDestination() + "\n\n"
            + "Return ONLY valid JSON (no markdown, no explanation, no code blocks) in this exact structure:\n"
            + "{\n"
            + "  \"mood\": \"<single word mood>\",\n"
            + "  \"energyLevel\": \"<low|medium|high>\",\n"
            + "  \"tripType\": \"<calm|adventure|heritage|food|nature|spiritual|shopping|budget|romantic>\",\n"
            + "  \"avoid\": [\"<thing1>\", \"<thing2>\"],\n"
            + "  \"preferredPlaces\": [\"<place1>\", \"<place2>\", \"<place3>\"],\n"
            + "  \"foodPreference\": \"<one line food preference>\",\n"
            + "  \"routeStyle\": \"<one line route style>\",\n"
            + "  \"suggestionReason\": \"<2 sentence explanation>\"\n"
            + "}\n\n"
            + "Only return the JSON object. No other text.";
    }

    /** Strip markdown code fences from Gemini output */
    private String extractJson(String raw) {
        if (raw == null) return "{}";
        String cleaned = raw.trim();
        // Remove ```json ... ``` or ``` ... ```
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('\n');
            int end = cleaned.lastIndexOf("```");
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start + 1, end).trim();
            }
        }
        // Find first { and last }
        int first = cleaned.indexOf('{');
        int last  = cleaned.lastIndexOf('}');
        if (first >= 0 && last > first) {
            cleaned = cleaned.substring(first, last + 1);
        }
        return cleaned;
    }

    /** Parse Gemini JSON response into MoodAnalysis, with field-level fallbacks */
    private MoodAnalysis parseGeminiJson(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        MoodAnalysis analysis = new MoodAnalysis();

        analysis.setMood(node.path("mood").asText("relaxed"));
        analysis.setEnergyLevel(node.path("energyLevel").asText("medium"));
        analysis.setTripType(node.path("tripType").asText("calm"));
        analysis.setFoodPreference(node.path("foodPreference").asText("local cuisine"));
        analysis.setRouteStyle(node.path("routeStyle").asText("comfortable and scenic"));
        analysis.setSuggestionReason(node.path("suggestionReason").asText("Enjoy a pleasant trip!"));

        List<String> avoid = new ArrayList<>();
        if (node.has("avoid") && node.get("avoid").isArray()) {
            node.get("avoid").forEach(e -> avoid.add(e.asText()));
        }
        analysis.setAvoid(avoid);

        List<String> preferred = new ArrayList<>();
        if (node.has("preferredPlaces") && node.get("preferredPlaces").isArray()) {
            node.get("preferredPlaces").forEach(e -> preferred.add(e.asText()));
        }
        analysis.setPreferredPlaces(preferred);

        return analysis;
    }

    /** Map tripType to a curated list of suggested place categories */
    private List<String> buildRecommendedPlaces(MoodAnalysis analysis) {
        String tripType = analysis.getTripType() != null ? analysis.getTripType().toLowerCase() : "calm";
        switch (tripType) {
            case "adventure":
                return Arrays.asList("Trekking Trails", "Viewpoints", "Water Sports", "Cycling Paths", "Rock Climbing");
            case "heritage":
                return Arrays.asList("Historical Forts", "Ancient Temples", "Museums", "Monuments", "Archaeological Sites");
            case "food":
                return Arrays.asList("Famous Local Restaurants", "Street Food Markets", "Rooftop Cafes", "Bakeries", "Biryani Spots");
            case "nature":
                return Arrays.asList("National Parks", "Waterfalls", "Lakes", "Forest Trails", "Botanical Gardens");
            case "spiritual":
                return Arrays.asList("Temples", "Mosques", "Churches", "Meditation Centers", "Pilgrimage Sites");
            case "shopping":
                return Arrays.asList("Local Markets", "Shopping Malls", "Handicraft Stores", "Bazaars", "Souvenir Shops");
            case "budget":
                return Arrays.asList("Free Attractions", "Budget Dhabas", "Public Parks", "Street Food", "Budget Hotels");
            case "romantic":
                return Arrays.asList("Sunset Points", "Lakeside Cafes", "Botanical Gardens", "Beach Walks", "Candlelight Restaurants");
            case "calm":
            default:
                return Arrays.asList("Quiet Parks", "Lakeside Gardens", "Small Cafes", "Local Temples", "Scenic Viewpoints");
        }
    }

    /** Generate readable trip advice from mood analysis */
    private String buildTripAdvice(MoodAnalysis analysis, MoodPlanRequest req) {
        StringBuilder advice = new StringBuilder();
        advice.append("Based on your ").append(analysis.getMood()).append(" mood, we recommend a ")
              .append(analysis.getRouteStyle()).append(" journey from ").append(req.getSource())
              .append(" to ").append(req.getDestination()).append(". ");
        if (analysis.getSuggestionReason() != null) {
            advice.append(analysis.getSuggestionReason());
        }
        if (analysis.getFoodPreference() != null) {
            advice.append(" For food, prefer: ").append(analysis.getFoodPreference()).append(".");
        }
        return advice.toString();
    }

    /** Rule-based fallback when Gemini is unavailable */
    private MoodPlanResponse buildFallbackResponse(MoodPlanRequest req) {
        MoodAnalysis fallbackAnalysis = new MoodAnalysis();
        String selected = req.getSelectedMood() != null ? req.getSelectedMood().toLowerCase() : "calm";

        switch (selected) {
            case "adventure":
                fallbackAnalysis.setMood("adventurous"); fallbackAnalysis.setEnergyLevel("high");
                fallbackAnalysis.setTripType("adventure");
                fallbackAnalysis.setAvoid(Arrays.asList("crowded malls", "boring routes"));
                fallbackAnalysis.setPreferredPlaces(Arrays.asList("viewpoints", "trekking", "sports"));
                fallbackAnalysis.setFoodPreference("high protein energy food");
                fallbackAnalysis.setRouteStyle("long and exciting");
                break;
            case "heritage":
                fallbackAnalysis.setMood("curious"); fallbackAnalysis.setEnergyLevel("medium");
                fallbackAnalysis.setTripType("heritage");
                fallbackAnalysis.setAvoid(Arrays.asList("nightclubs", "shopping malls"));
                fallbackAnalysis.setPreferredPlaces(Arrays.asList("museums", "forts", "monuments"));
                fallbackAnalysis.setFoodPreference("traditional local food");
                fallbackAnalysis.setRouteStyle("culturally rich and informative");
                break;
            case "food":
                fallbackAnalysis.setMood("hungry"); fallbackAnalysis.setEnergyLevel("medium");
                fallbackAnalysis.setTripType("food");
                fallbackAnalysis.setAvoid(Arrays.asList("long trekking", "extreme activities"));
                fallbackAnalysis.setPreferredPlaces(Arrays.asList("restaurants", "street food", "cafes"));
                fallbackAnalysis.setFoodPreference("cheap and highly rated local cuisine");
                fallbackAnalysis.setRouteStyle("food trail focused");
                break;
            case "budget":
                fallbackAnalysis.setMood("frugal"); fallbackAnalysis.setEnergyLevel("medium");
                fallbackAnalysis.setTripType("budget");
                fallbackAnalysis.setAvoid(Arrays.asList("expensive hotels", "luxury restaurants"));
                fallbackAnalysis.setPreferredPlaces(Arrays.asList("free parks", "budget dhabas", "public beaches"));
                fallbackAnalysis.setFoodPreference("affordable street food");
                fallbackAnalysis.setRouteStyle("cost-effective and efficient");
                break;
            case "relaxing":
            default:
                fallbackAnalysis.setMood("relaxed"); fallbackAnalysis.setEnergyLevel("low");
                fallbackAnalysis.setTripType("calm");
                fallbackAnalysis.setAvoid(Arrays.asList("crowded places", "long walking"));
                fallbackAnalysis.setPreferredPlaces(Arrays.asList("parks", "cafes", "scenic viewpoints"));
                fallbackAnalysis.setFoodPreference("light and comfortable");
                fallbackAnalysis.setRouteStyle("short and comfortable");
        }
        fallbackAnalysis.setSuggestionReason("AI mood planning used preset rules. Showing best matches for your mood.");

        MoodPlanResponse response = new MoodPlanResponse();
        response.setMoodAnalysis(fallbackAnalysis);
        response.setRecommendedPlaces(buildRecommendedPlaces(fallbackAnalysis));
        response.setTripAdvice("Showing trip plan based on your selected mood: " + req.getSelectedMood() + ".");
        response.setFallback(true);
        response.setFallbackMessage("AI mood planning is currently unavailable. Showing normal trip plan.");
        return response;
    }
}
