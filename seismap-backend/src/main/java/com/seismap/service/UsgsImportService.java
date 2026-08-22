package com.seismap.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.seismap.model.entity.UsgsEvent;
import com.seismap.repository.UsgsEventRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Imports historical earthquakes for Argentina from the public USGS FDSN
 * Event Web Service into the dedicated `usgs_event` table (see V7 migration).
 * Kept separate from the Nordic/SEISAN-parsed `event` catalog on purpose —
 * see UsgsEvent's javadoc.
 */
@Service
public class UsgsImportService {

    private static final Logger log = LoggerFactory.getLogger(UsgsImportService.class);

    // SRID 900913 = Spherical Mercator (same as EPSG:3857) — matches Event's storage
    private static final int SRID = 900913;
    private static final double ORIGIN_SHIFT = 2 * Math.PI * 6378137 / 2.0;

    // Bounding box roughly covering continental Argentina + Tierra del Fuego
    private static final double MIN_LON = -73.6;
    private static final double MAX_LON = -53.0;
    private static final double MIN_LAT = -55.5;
    private static final double MAX_LAT = -21.0;
    private static final double MIN_MAGNITUDE = 3.0;
    private static final int YEARS_OF_HISTORY = 20;

    private final UsgsEventRepository repository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
    private final RestClient restClient = RestClient.create();

    public UsgsImportService(UsgsEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Fetches the last {@value #YEARS_OF_HISTORY} years of magnitude >=
     * {@value #MIN_MAGNITUDE} earthquakes within Argentina's bounding box,
     * in yearly chunks (USGS caps a single query at 20,000 events). Saving is
     * an upsert keyed by the USGS event id, so running this again just
     * refreshes/extends the existing data.
     */
    @Transactional
    public Map<String, Object> importHistoricalEvents() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(YEARS_OF_HISTORY);

        int fetched = 0;
        LocalDate chunkStart = start;
        while (chunkStart.isBefore(end)) {
            LocalDate chunkEnd = chunkStart.plusYears(1).isAfter(end) ? end : chunkStart.plusYears(1);
            List<UsgsEvent> events = fetchRange(chunkStart, chunkEnd);
            repository.saveAll(events);
            fetched += events.size();
            log.info("USGS import: {} events from {} to {}", events.size(), chunkStart, chunkEnd);
            chunkStart = chunkEnd;
        }

        long total = repository.count();
        log.info("USGS import complete: {} events fetched this run, {} total in usgs_event", fetched, total);
        return Map.of("fetched", fetched, "totalInTable", total);
    }

    private List<UsgsEvent> fetchRange(LocalDate from, LocalDate to) {
        String url = "https://earthquake.usgs.gov/fdsnws/event/1/query"
                + "?format=geojson"
                + "&starttime=" + from
                + "&endtime=" + to
                + "&minlatitude=" + MIN_LAT + "&maxlatitude=" + MAX_LAT
                + "&minlongitude=" + MIN_LON + "&maxlongitude=" + MAX_LON
                + "&minmagnitude=" + MIN_MAGNITUDE
                + "&limit=20000";

        UsgsFeatureCollection collection = restClient.get()
                .uri(url)
                .retrieve()
                .body(UsgsFeatureCollection.class);

        if (collection == null || collection.features() == null) {
            return List.of();
        }

        return collection.features().stream()
                .filter(this::hasRequiredFields)
                .map(this::toEntity)
                .toList();
    }

    private boolean hasRequiredFields(UsgsFeature f) {
        return f.id() != null
                && f.properties() != null
                && f.properties().time() != null
                && f.geometry() != null
                && f.geometry().coordinates() != null
                && f.geometry().coordinates().size() >= 2;
    }

    private UsgsEvent toEntity(UsgsFeature f) {
        List<Double> coords = f.geometry().coordinates();
        double lon = coords.get(0);
        double lat = coords.get(1);
        double depthKm = coords.size() > 2 && coords.get(2) != null ? coords.get(2) : 0.0;

        double[] mercator = latLonToSphericalMercator(lat, lon);
        Point location = geometryFactory.createPoint(new Coordinate(mercator[0], mercator[1]));
        location.setSRID(SRID);

        UsgsEvent event = new UsgsEvent();
        event.setId(f.id());
        event.setLocation(location);
        event.setDepth((float) depthKm);
        event.setDate(LocalDateTime.ofInstant(Instant.ofEpochMilli(f.properties().time()), ZoneOffset.UTC));
        event.setMagnitude(f.properties().mag() != null ? f.properties().mag().floatValue() : null);
        event.setMagnitudeType(f.properties().magType());
        event.setPlace(f.properties().place());
        event.setUrl(f.properties().url());
        return event;
    }

    /** Same conversion DataLoadService uses for the Nordic/SEISAN catalog. */
    private static double[] latLonToSphericalMercator(double lat, double lon) {
        double x = lon * ORIGIN_SHIFT / 180.0;
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0);
        y = y * ORIGIN_SHIFT / 180.0;
        return new double[] { x, y };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UsgsFeatureCollection(List<UsgsFeature> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UsgsFeature(String id, UsgsProperties properties, UsgsGeometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UsgsProperties(Long time, Double mag, String magType, String place, String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UsgsGeometry(List<Double> coordinates) {
    }
}
