package com.vyntra.repository;

import com.vyntra.model.RemovedPlace;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RemovedPlaceRepository extends MongoRepository<RemovedPlace, String> {
    List<RemovedPlace> findByTripId(String tripId);
}
