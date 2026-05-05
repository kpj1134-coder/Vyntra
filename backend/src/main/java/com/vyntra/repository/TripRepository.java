package com.vyntra.repository;

import com.vyntra.model.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TripRepository extends MongoRepository<Trip, String> {
    List<Trip> findByUserIdOrderByCreatedAtDesc(String userId);
}
