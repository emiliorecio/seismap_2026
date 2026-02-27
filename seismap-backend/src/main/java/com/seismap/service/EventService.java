package com.seismap.service;

import com.seismap.dto.EventSummaryDto;
import com.seismap.dto.PolygonQueryRequest;
import com.seismap.model.entity.Event;
import com.seismap.model.entity.DataBounds;
import com.seismap.model.entity.MagnitudeLimits;
import com.seismap.repository.EventRepository;
import com.seismap.repository.DataBoundsRepository;
import com.seismap.repository.MagnitudeLimitsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final DataBoundsRepository dataBoundsRepository;
    private final MagnitudeLimitsRepository magnitudeLimitsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EventService(EventRepository eventRepository,
            DataBoundsRepository dataBoundsRepository,
            MagnitudeLimitsRepository magnitudeLimitsRepository) {
        this.eventRepository = eventRepository;
        this.dataBoundsRepository = dataBoundsRepository;
        this.magnitudeLimitsRepository = magnitudeLimitsRepository;
    }

    public Event getById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
    }

    @Transactional
    public Event update(Long id, Event updated) {
        Event event = getById(id);
        event.setName(updated.getName());
        event.setNotes(updated.getNotes());
        event.setReference(updated.getReference());
        event.setPerceivedDistance(updated.getPerceivedDistance());
        event.setDamagedDistance(updated.getDamagedDistance());
        return eventRepository.save(event);
    }

    public DataBounds getDataBounds() {
        return dataBoundsRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("DataBounds not found"));
    }

    public List<MagnitudeLimits> getMagnitudeLimits() {
        return magnitudeLimitsRepository.findAll();
    }

    @SuppressWarnings("unchecked")
    public Page<EventSummaryDto> findWithinPolygon(PolygonQueryRequest request) {
        StringBuilder baseWhere = new StringBuilder(
                "FROM eventandaveragemagnitudes " +
                        "WHERE ST_Within(location, ST_GeomFromText(:wkt, 900913)) ");

        if (request.getMinDate() != null)
            baseWhere.append("AND date >= :minDate ");
        if (request.getMaxDate() != null)
            baseWhere.append("AND date <= :maxDate ");
        if (request.getMinDepth() != null)
            baseWhere.append("AND depth >= :minDepth ");
        if (request.getMaxDepth() != null)
            baseWhere.append("AND depth <= :maxDepth ");
        if (request.getMinMagnitude() != null)
            baseWhere.append("AND rankmagnitude >= :minMagnitude ");
        if (request.getMaxMagnitude() != null)
            baseWhere.append("AND rankmagnitude <= :maxMagnitude ");

        // Count Query
        Query countQuery = entityManager.createNativeQuery("SELECT count(*) " + baseWhere.toString());

        // Data Query
        StringBuilder dataSql = new StringBuilder(
                "SELECT id, date, depth, ST_Y(location) as lat, ST_X(location) as lon, name, reference, rankmagnitude ")
                .append(baseWhere).append("ORDER BY date DESC LIMIT :limit OFFSET :offset");

        Query dataQuery = entityManager.createNativeQuery(dataSql.toString());

        // Set parameters for both queries
        countQuery.setParameter("wkt", request.getWkt());
        dataQuery.setParameter("wkt", request.getWkt());
        dataQuery.setParameter("limit", request.getSize());
        dataQuery.setParameter("offset", request.getPage() * request.getSize());

        if (request.getMinDate() != null) {
            countQuery.setParameter("minDate", request.getMinDate());
            dataQuery.setParameter("minDate", request.getMinDate());
        }
        if (request.getMaxDate() != null) {
            countQuery.setParameter("maxDate", request.getMaxDate());
            dataQuery.setParameter("maxDate", request.getMaxDate());
        }
        if (request.getMinDepth() != null) {
            countQuery.setParameter("minDepth", request.getMinDepth());
            dataQuery.setParameter("minDepth", request.getMinDepth());
        }
        if (request.getMaxDepth() != null) {
            countQuery.setParameter("maxDepth", request.getMaxDepth());
            dataQuery.setParameter("maxDepth", request.getMaxDepth());
        }
        if (request.getMinMagnitude() != null) {
            countQuery.setParameter("minMagnitude", request.getMinMagnitude());
            dataQuery.setParameter("minMagnitude", request.getMinMagnitude());
        }
        if (request.getMaxMagnitude() != null) {
            countQuery.setParameter("maxMagnitude", request.getMaxMagnitude());
            dataQuery.setParameter("maxMagnitude", request.getMaxMagnitude());
        }

        long total = ((Number) countQuery.getSingleResult()).longValue();
        List<Object[]> results = dataQuery.getResultList();

        List<EventSummaryDto> dtoList = results.stream().map(row -> new EventSummaryDto(
                ((Number) row[0]).longValue(),
                ((java.sql.Timestamp) row[1]).toLocalDateTime(),
                ((Number) row[2]).floatValue(),
                ((Number) row[3]).doubleValue(),
                ((Number) row[4]).doubleValue(),
                (String) row[5],
                (String) row[6],
                row[7] != null ? ((Number) row[7]).floatValue() : null)).toList();

        return new PageImpl<>(dtoList, PageRequest.of(request.getPage(), request.getSize()), total);
    }
}
