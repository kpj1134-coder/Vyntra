package com.vyntra.repository;

import com.vyntra.model.SuggestedPlace;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface SuggestedPlaceRepository extends MongoRepository<SuggestedPlace, String> {
    List<SuggestedPlace> findByTripIdOrderByStopOrderAsc(String tripId);
}
