package com.seismap.model.entity;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

/**
 * Base class for event data, used by both Event and EventAndAverageMagnitudes.
 */
@MappedSuperclass
public abstract class EventInfo {

    @Column(nullable = false, columnDefinition = "geometry(Point, 900913)")
    private Point location;

    @Column(nullable = false)
    private float depth;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column
    private String name;

    @Column
    private String notes;

    @Column
    private String reference;

    @Column(name = "perceived_distance")
    private Integer perceivedDistance;

    @Column(name = "damaged_distance")
    private Integer damagedDistance;

    protected EventInfo() {
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public double getLatitude() {
        return location.getY();
    }

    public double getLongitude() {
        return location.getX();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Integer getPerceivedDistance() {
        return perceivedDistance;
    }

    public void setPerceivedDistance(Integer perceivedDistance) {
        this.perceivedDistance = perceivedDistance;
    }

    public Integer getDamagedDistance() {
        return damagedDistance;
    }

    public void setDamagedDistance(Integer damagedDistance) {
        this.damagedDistance = damagedDistance;
    }
}
