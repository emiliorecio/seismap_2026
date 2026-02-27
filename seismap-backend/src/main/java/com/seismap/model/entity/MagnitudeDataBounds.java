package com.seismap.model.entity;

import com.seismap.model.enums.ExtendedMagnitudeType;
import jakarta.persistence.*;

@Entity
@Table(name = "magnitude_data_bounds")
public class MagnitudeDataBounds {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "magnitude_type", nullable = false)
    private ExtendedMagnitudeType magnitudeType;

    @Column
    private Float min;

    @Column
    private Float max;

    protected MagnitudeDataBounds() {
    }

    public ExtendedMagnitudeType getMagnitudeType() {
        return magnitudeType;
    }

    public Float getMin() {
        return min;
    }

    public Float getMax() {
        return max;
    }
}
