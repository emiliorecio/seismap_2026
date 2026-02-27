package com.seismap.model.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "event")
public class Event extends EventInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "event_id", nullable = false)
    private List<Magnitude> magnitudes = new ArrayList<>();

    public Event() {
    }

    public Long getId() {
        return id;
    }

    public List<Magnitude> getMagnitudes() {
        return Collections.unmodifiableList(magnitudes);
    }

    public void addMagnitude(Magnitude magnitude) {
        magnitudes.add(magnitude);
    }
}
