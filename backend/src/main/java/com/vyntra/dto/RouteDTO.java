package com.vyntra.dto;

import java.util.List;

public class RouteDTO {
    private Double totalDistanceKm;
    private String totalDuration;
    private List<double[]> waypoints;
    private String polyline;

    public RouteDTO() {}

    public Double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(Double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public String getTotalDuration() { return totalDuration; }
    public void setTotalDuration(String totalDuration) { this.totalDuration = totalDuration; }

    public List<double[]> getWaypoints() { return waypoints; }
    public void setWaypoints(List<double[]> waypoints) { this.waypoints = waypoints; }

    public String getPolyline() { return polyline; }
    public void setPolyline(String polyline) { this.polyline = polyline; }
}
