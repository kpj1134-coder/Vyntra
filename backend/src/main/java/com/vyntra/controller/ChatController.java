package com.vyntra.controller;

import com.vyntra.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String message = body.getOrDefault("message", "").toLowerCase().trim();
        String tripId = body.getOrDefault("tripId", null);
        String reply = generateReply(message, tripId);
        Map<String, String> response = new HashMap<>();
        response.put("reply", reply);
        return ResponseEntity.ok(response);
    }

    private String generateReply(String message, String tripId) {
        if (message.isEmpty()) {
            return "Hi! I'm Vyntra AI Assistant. Ask me about trip planning, rescue mode, or how our scoring works!";
        }

        if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            return "Hello! 👋 I'm your Vyntra travel assistant. I can help you plan trips, explain how places are scored, or guide you through rescue mode. What would you like to know?";
        }

        if (message.contains("plan") || message.contains("trip")) {
            return "To plan a trip:\n1. Go to 'Plan Trip' from the sidebar\n2. Enter your start location and destination (any city!)\n3. Set your budget, time, and energy level\n4. Select your interests (food, nature, temples, etc.)\n5. Click 'Generate Trip Plan'\n\nVyntra will find the best stops along your route, filtered by your constraints!";
        }

        if (message.contains("rescue") || message.contains("replan") || message.contains("emergency")) {
            return "Rescue Mode helps when things go wrong:\n• Rain ruining outdoor plans? It removes outdoor stops and suggests indoor alternatives\n• Too tired? It finds nearby rest stops and cafes\n• Budget running low? It filters expensive places\n\nGo to 'Rescue Mode' from the sidebar, enter your current location and issue, and get an instant replan!";
        }

        if (message.contains("score") || message.contains("scoring") || message.contains("rank")) {
            return "Our scoring formula:\n📊 Score = (Rating × 20) - (Route Deviation × 15) - (Cost × 0.05) + Interest Match Bonus (+25) + Weather Bonus (+15) + Open Now Bonus (+10) - Energy Penalty\n\nHigher scores mean better matches for your specific constraints. Each place shows its score and reason for selection.";
        }

        if (message.contains("why") || message.contains("selected") || message.contains("reason")) {
            return "Every selected place has a detailed reason explaining why it was chosen. This includes:\n• How close it is to your route\n• Whether it matches your interests\n• Its rating and cost\n• Weather suitability\n• Whether it's currently open\n\nCheck the trip result page for per-place explanations!";
        }

        if (message.contains("weather")) {
            return "Vyntra checks real-time weather at your destination. If it's rainy:\n🌧️ Outdoor places (parks, lakes, viewpoints) are automatically removed\n🏛️ Indoor alternatives (museums, cafes, temples) get bonus scores\n\nYou can also use Rescue Mode if weather changes mid-trip!";
        }

        if (message.contains("budget")) {
            return "Budget constraints work like this:\n• If your budget is under ₹500, expensive places (>₹300 per stop) are filtered out\n• Each place has an estimated cost shown on the result page\n• The total estimated stop cost is displayed in the trip header\n• Remaining budget is calculated in the AI summary";
        }

        if (message.contains("suggest") || message.contains("place")) {
            return "I find stops along your route using these categories based on your interests:\n🍽️ Food → restaurants, cafes\n🕉️ Temple → temples, shrines\n🌳 Nature → parks, viewpoints\n🛍️ Shopping → malls, markets\n☕ Rest → coffee shops, rest stops\n🏥 Emergency → hospitals, ATMs\n\nAll places are within 2km of your route!";
        }

        if (message.contains("history") || message.contains("saved")) {
            return "Your trip history shows all past planned trips. Each trip can be:\n• Viewed with full details (stops, scores, AI summary)\n• Saved as an itinerary for future reference\n\nGo to 'History' in the sidebar to see all your trips!";
        }

        if (message.contains("constraint") || message.contains("filter")) {
            return "The Constraint Engine filters places based on:\n🌧️ Rain → removes outdoor places\n💰 Low Budget → removes expensive stops\n😴 Low Energy → removes far/strenuous places\n⏰ Limited Time → reduces max stops\n👨‍👩‍👧‍👦 Family → removes bars/pubs\n📏 Route Deviation → removes places >2km from route\n\nRemoved places are shown with reasons on the result page.";
        }

        return "I can help you with:\n• How to plan a trip\n• How scoring works\n• Understanding constraints\n• Using rescue mode\n• Weather effects\n• Budget planning\n\nJust ask me anything about your travel planning!";
    }
}
