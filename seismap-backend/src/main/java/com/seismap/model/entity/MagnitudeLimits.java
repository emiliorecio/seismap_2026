package com.seismap.model.entity;

import com.seismap.model.enums.ExtendedMagnitudeType;
import jakarta.persistence.*;

@Entity
@Table(name = "magnitude_limits")
public class MagnitudeLimits {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "magnitude_type", nullable = false)
    private ExtendedMagnitudeType magnitudeType;

    @Column(nullable = false)
    private float min;

    @Column(nullable = false)
    private float max;

    protected MagnitudeLimits() {
    }

    public ExtendedMagnitudeType getMagnitudeType() {
        return magnitudeType;
    }

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }
}
