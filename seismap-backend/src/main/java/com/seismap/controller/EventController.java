package com.seismap.controller;

import com.seismap.dto.EventSummaryDto;
import com.seismap.dto.PolygonQueryRequest;
import com.seismap.model.entity.Event;
import com.seismap.model.entity.DataBounds;
import com.seismap.model.entity.MagnitudeLimits;
import com.seismap.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{id}")
    public Event get(@PathVariable Long id) {
        return eventService.getById(id);
    }

    @PutMapping("/{id}")
    public Event update(@PathVariable Long id, @RequestBody Event event) {
        return eventService.update(id, event);
    }

    @GetMapping("/data-bounds")
    public DataBounds getDataBounds() {
        return eventService.getDataBounds();
    }

    @GetMapping("/magnitude-limits")
    public List<MagnitudeLimits> getMagnitudeLimits() {
        return eventService.getMagnitudeLimits();
    }

    /**
     * Returns all events within the given polygon (WKT, EPSG:900913).
     * Body: { "wkt": "POLYGON((x1 y1, x2 y2, ...))", "page": 0, "size": 50 }
     */
    @PostMapping("/within")
    public Page<EventSummaryDto> findWithin(@RequestBody PolygonQueryRequest request) {
        return eventService.findWithinPolygon(request);
    }
}
