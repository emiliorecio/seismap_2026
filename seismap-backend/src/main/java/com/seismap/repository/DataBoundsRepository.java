package com.seismap.repository;

import com.seismap.model.entity.DataBounds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataBoundsRepository extends JpaRepository<DataBounds, Long> {
}
