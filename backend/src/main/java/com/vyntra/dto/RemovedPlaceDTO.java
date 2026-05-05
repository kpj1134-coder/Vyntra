package com.vyntra.dto;

public class RemovedPlaceDTO {
    private String name;
    private String reasonRemoved;

    public RemovedPlaceDTO() {}

    public RemovedPlaceDTO(String name, String reasonRemoved) {
        this.name = name;
        this.reasonRemoved = reasonRemoved;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReasonRemoved() { return reasonRemoved; }
    public void setReasonRemoved(String reasonRemoved) { this.reasonRemoved = reasonRemoved; }
}
