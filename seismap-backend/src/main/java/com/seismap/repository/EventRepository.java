package com.seismap.repository;

import com.seismap.model.entity.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = { "magnitudes", "magnitudes.reportingAgency" })
    Optional<Event> findById(Long id);

    /**
     * Returns all events whose location falls within the given WKT polygon
     * (EPSG:900913).
     * Joins with the eventandaveragemagnitudes view to get rankindex as
     * approximation of magnitude.
     */
    @Query(value = """
            SELECT e.* FROM event e
            WHERE ST_Within(e.location, ST_GeomFromText(:wkt, 900913))
            """, nativeQuery = true)
    List<Event> findWithinPolygon(@Param("wkt") String wkt);
}
