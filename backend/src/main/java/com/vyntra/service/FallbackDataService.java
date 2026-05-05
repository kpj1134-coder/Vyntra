package com.vyntra.service;

import com.vyntra.dto.PlaceDTO;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Provides curated fallback demo data for the Mysore route
 * and generic city demos when API keys are unavailable.
 */
@Service
public class FallbackDataService {

    /**
     * Returns curated Mysore demo places for:
     * Start: Mysore Railway Station → Destination: Brindavan Gardens
     */
    public List<PlaceDTO> getMysoreDemo() {
        List<PlaceDTO> places = new ArrayList<>();

        PlaceDTO p1 = new PlaceDTO();
        p1.setName("St. Philomena's Cathedral");
        p1.setCategory("temple");
        p1.setAddress("Lashkar Mohalla, Mysuru");
        p1.setRating(4.6);
        p1.setEstimatedCost(0.0);
        p1.setDistanceFromRoute(0.8);
        p1.setOpenNow(true);
        p1.setLatitude(12.3178);
        p1.setLongitude(76.6553);
        places.add(p1);

        PlaceDTO p2 = new PlaceDTO();
        p2.setName("Mysore Palace");
        p2.setCategory("temple");
        p2.setAddress("Sayyaji Rao Rd, Agrahara, Mysuru");
        p2.setRating(4.8);
        p2.setEstimatedCost(70.0);
        p2.setDistanceFromRoute(0.5);
        p2.setOpenNow(true);
        p2.setLatitude(12.3052);
        p2.setLongitude(76.6552);
        places.add(p2);

        PlaceDTO p3 = new PlaceDTO();
        p3.setName("Karanji Lake Nature Park");
        p3.setCategory("nature");
        p3.setAddress("Leonard Hall Rd, Mysuru");
        p3.setRating(4.3);
        p3.setEstimatedCost(20.0);
        p3.setDistanceFromRoute(1.2);
        p3.setOpenNow(true);
        p3.setLatitude(12.2975);
        p3.setLongitude(76.6604);
        places.add(p3);

        PlaceDTO p4 = new PlaceDTO();
        p4.setName("Hotel RRR (Mysore Thali)");
        p4.setCategory("food");
        p4.setAddress("Gandhi Square, Mysuru");
        p4.setRating(4.4);
        p4.setEstimatedCost(200.0);
        p4.setDistanceFromRoute(0.3);
        p4.setOpenNow(true);
        p4.setLatitude(12.3084);
        p4.setLongitude(76.6520);
        places.add(p4);

        PlaceDTO p5 = new PlaceDTO();
        p5.setName("KRS Dam Viewpoint");
        p5.setCategory("nature");
        p5.setAddress("Krishna Raja Sagara, near Brindavan Gardens");
        p5.setRating(4.5);
        p5.setEstimatedCost(0.0);
        p5.setDistanceFromRoute(0.6);
        p5.setOpenNow(true);
        p5.setLatitude(12.4244);
        p5.setLongitude(76.5728);
        places.add(p5);

        PlaceDTO p6 = new PlaceDTO();
        p6.setName("Cafe Aramane");
        p6.setCategory("rest");
        p6.setAddress("Devaraja Urs Rd, Mysuru");
        p6.setRating(4.2);
        p6.setEstimatedCost(150.0);
        p6.setDistanceFromRoute(0.4);
        p6.setOpenNow(true);
        p6.setLatitude(12.3098);
        p6.setLongitude(76.6560);
        places.add(p6);

        return places;
    }

    /**
     * Check if the trip is a Mysore demo trip.
     */
    public boolean isMysoreDemo(String start, String destination) {
        if (start == null || destination == null) return false;
        String s = start.toLowerCase();
        String d = destination.toLowerCase();
        return (s.contains("mysore") || s.contains("mysuru")) &&
               (d.contains("brindavan") || d.contains("krs") || d.contains("mysore") || d.contains("mysuru"));
    }
}
