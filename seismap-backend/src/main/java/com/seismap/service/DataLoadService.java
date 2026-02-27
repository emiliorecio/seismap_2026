package com.seismap.service;

import com.seismap.model.entity.Agency;
import com.seismap.model.entity.Event;
import com.seismap.model.entity.Magnitude;
import com.seismap.repository.AgencyRepository;
import com.seismap.repository.EventRepository;
import com.seismap.service.parser.NordicSeisanParser;
import com.seismap.service.parser.NordicSeisanParser.ParsedEvent;
import com.seismap.service.parser.NordicSeisanParser.ParsedMagnitude;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class DataLoadService {

    private static final Logger log = LoggerFactory.getLogger(DataLoadService.class);

    // SRID 900913 = Spherical Mercator (same as EPSG:3857)
    private static final int SRID = 900913;
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0; // ~20037508.34

    @Value("${seismap.data-files-directory:./data}")
    private String dataFilesDirectory;

    private final EventRepository eventRepository;
    private final AgencyRepository agencyRepository;
    private final GeometryFactory geometryFactory;
    private final NordicSeisanParser parser;

    // Cache for agencies to avoid repeated DB lookups
    private final Map<String, Agency> agencyCache = new HashMap<>();

    public DataLoadService(EventRepository eventRepository, AgencyRepository agencyRepository) {
        this.eventRepository = eventRepository;
        this.agencyRepository = agencyRepository;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
        this.parser = new NordicSeisanParser();
    }

    public List<File> listDataFiles() {
        File dir = new File(dataFilesDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = dir.listFiles((d, name) -> name.endsWith(".data"));
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    @Transactional
    public Map<String, Object> loadDataFile(String filename) throws IOException {
        File dir = new File(dataFilesDirectory);
        File[] matches = dir.listFiles((d, name) -> name.equals(filename));
        if (matches == null || matches.length == 0) {
            throw new IllegalArgumentException("No such data file: " + filename);
        }

        File file = matches[0];
        log.info("Starting to parse file: {}", file.getAbsolutePath());

        // Parse the file
        List<ParsedEvent> parsedEvents = parser.parseFile(file);
        log.info("Parsed {} events from file {}", parsedEvents.size(), filename);

        // Reset agency cache for this load
        agencyCache.clear();
        agencyRepository.findAll().forEach(a -> agencyCache.put(a.getCode(), a));

        int loaded = 0;
        int skipped = 0;

        for (ParsedEvent pe : parsedEvents) {
            try {
                Event event = createEvent(pe);
                eventRepository.save(event);
                loaded++;
            } catch (Exception e) {
                log.warn("Skipping event at {}: {}", pe.getDate(), e.getMessage());
                skipped++;
            }
        }

        log.info("Load complete: {} loaded, {} skipped from file {}", loaded, skipped, filename);

        return Map.of(
                "file", filename,
                "parsed", parsedEvents.size(),
                "loaded", loaded,
                "skipped", skipped);
    }

    private Event createEvent(ParsedEvent pe) {
        // Convert lat/lon (WGS84) to Spherical Mercator (EPSG:900913)
        double[] mercator = latLonToSphericalMercator(pe.getLatitude(), pe.getLongitude());

        Point location = geometryFactory.createPoint(new Coordinate(mercator[0], mercator[1]));
        location.setSRID(SRID);

        Event event = new Event();
        event.setLocation(location);
        event.setDepth(pe.getDepth());
        event.setDate(pe.getDate());

        // Add magnitudes
        for (ParsedMagnitude pm : pe.getMagnitudes()) {
            Agency agency = getOrCreateAgency(pm.getReportingAgency());
            event.addMagnitude(new Magnitude(agency, pm.getType(), pm.getValue()));
        }

        return event;
    }

    private Agency getOrCreateAgency(String code) {
        return agencyCache.computeIfAbsent(code, c -> {
            Optional<Agency> existing = agencyRepository.findByCode(c);
            return existing.orElseGet(() -> agencyRepository.save(new Agency(c)));
        });
    }

    /**
     * Convert latitude/longitude (WGS84, degrees) to Spherical Mercator
     * (EPSG:900913) coordinates.
     * This matches the legacy CoordinatesConverter logic.
     *
     * @return double[] {x, y} in meters
     */
    private static double[] latLonToSphericalMercator(double lat, double lon) {
        double x = lon * ORIGIN_SHIFT / 180.0;
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        y = y * ORIGIN_SHIFT / 180.0;
        return new double[] { x, y };
    }
}
