package com.seismap.dto;

import java.time.LocalDateTime;

public class EventSummaryDto {

    private Long id;
    private LocalDateTime date;
    private float depth;
    private double latitude;
    private double longitude;
    private String name;
    private String reference;
    private Float rankMagnitude;

    public EventSummaryDto() {
    }

    public EventSummaryDto(Long id, LocalDateTime date, float depth,
            double latitude, double longitude,
            String name, String reference, Float rankMagnitude) {
        this.id = id;
        this.date = date;
        this.depth = depth;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.reference = reference;
        this.rankMagnitude = rankMagnitude;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public float getDepth() {
        return depth;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public Float getRankMagnitude() {
        return rankMagnitude;
    }
}
