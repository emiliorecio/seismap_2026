package com.seismap.repository;

import com.seismap.model.entity.MagnitudeLimits;
import com.seismap.model.enums.ExtendedMagnitudeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MagnitudeLimitsRepository extends JpaRepository<MagnitudeLimits, ExtendedMagnitudeType> {
}
