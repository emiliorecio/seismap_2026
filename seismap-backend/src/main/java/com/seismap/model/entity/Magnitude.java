package com.seismap.model.entity;

import com.seismap.model.enums.MagnitudeType;
import jakarta.persistence.*;

@Entity
@Table(name = "magnitude")
public class Magnitude {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MagnitudeType type;

    @Column(nullable = false)
    private float value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporting_agency_id", nullable = false)
    private Agency reportingAgency;

    protected Magnitude() {
    }

    public Magnitude(Agency reportingAgency, MagnitudeType type, float value) {
        this.reportingAgency = reportingAgency;
        this.type = type;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public MagnitudeType getType() {
        return type;
    }

    public float getValue() {
        return value;
    }

    public Agency getReportingAgency() {
        return reportingAgency;
    }
}
