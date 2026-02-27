package com.seismap.model.entity;

import com.seismap.model.enums.ExtendedMagnitudeType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

@Entity
@Table(name = "data_bounds")
public class DataBounds {

    @Id
    private Long id;

    @Column(name = "min_date")
    private LocalDateTime minDate;

    @Column(name = "max_date")
    private LocalDateTime maxDate;

    @Column(name = "min_depth")
    private Float minDepth;

    @Column(name = "max_depth")
    private Float maxDepth;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "data_bound_id")
    @MapKey(name = "magnitudeType")
    private java.util.Map<ExtendedMagnitudeType, MagnitudeDataBounds> magnitudeBounds = new LinkedHashMap<>();

    protected DataBounds() {
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getMinDate() {
        return minDate;
    }

    public LocalDateTime getMaxDate() {
        return maxDate;
    }

    public Float getMinDepth() {
        return minDepth;
    }

    public Float getMaxDepth() {
        return maxDepth;
    }

    public java.util.Map<ExtendedMagnitudeType, MagnitudeDataBounds> getMagnitudeBounds() {
        return java.util.Collections.unmodifiableMap(magnitudeBounds);
    }
}
