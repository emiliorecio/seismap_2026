package com.seismap.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

/**
 * A historical earthquake imported from the USGS catalog. Kept as its own
 * table (not merged into `event`) since it's a different data source with a
 * different shape (one magnitude per event, no per-agency breakdown) and its
 * own GeoServer layer.
 */
@Entity
@Table(name = "usgs_event")
public class UsgsEvent {

    /** USGS event id, e.g. "us7000abcd" — stable, so re-importing upserts. */
    @Id
    private String id;

    @Column(nullable = false, columnDefinition = "geometry(Point, 900913)")
    private Point location;

    @Column(nullable = false)
    private float depth;

    @Column(nullable = false)
    private LocalDateTime date;

    private Float magnitude;

    @Column(name = "magnitude_type")
    private String magnitudeType;

    private String place;

    private String url;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public float getDepth() {
        return depth;
    }

    public void setDepth(float depth) {
        this.depth = depth;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Float getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(Float magnitude) {
        this.magnitude = magnitude;
    }

    public String getMagnitudeType() {
        return magnitudeType;
    }

    public void setMagnitudeType(String magnitudeType) {
        this.magnitudeType = magnitudeType;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
