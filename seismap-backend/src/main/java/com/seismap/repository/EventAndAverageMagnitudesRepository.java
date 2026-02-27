package com.seismap.repository;

import com.seismap.model.entity.EventAndAverageMagnitudes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAndAverageMagnitudesRepository extends JpaRepository<EventAndAverageMagnitudes, Long> {
}
