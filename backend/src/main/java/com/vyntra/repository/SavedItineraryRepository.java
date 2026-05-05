package com.vyntra.repository;

import com.vyntra.model.SavedItinerary;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SavedItineraryRepository extends MongoRepository<SavedItinerary, String> {
    Optional<SavedItinerary> findByTripId(String tripId);
}
