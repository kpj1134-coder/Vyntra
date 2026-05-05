package com.vyntra.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "removed_places")
public class RemovedPlace {

    @Id
    private String id;

    private String tripId;

    private String name;
    private String reasonRemoved;

    public RemovedPlace() {}

    public RemovedPlace(String tripId, String name, String reasonRemoved) {
        this.tripId = tripId;
        this.name = name;
        this.reasonRemoved = reasonRemoved;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReasonRemoved() { return reasonRemoved; }
    public void setReasonRemoved(String reasonRemoved) { this.reasonRemoved = reasonRemoved; }
}
