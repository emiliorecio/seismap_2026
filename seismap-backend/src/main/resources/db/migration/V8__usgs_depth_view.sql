-- V8: Depth-profile view for the USGS catalog, mirroring V5/V6's approach for
-- the local catalog: `depthlocation` is declared before `location` so
-- GeoServer picks it as the default/hit-test geometry, while `location` stays
-- available for the polygon selection's CQL_FILTER.
CREATE VIEW usgs_event_depth_live AS
SELECT
    id, date, depth, magnitude, magnitude_type, place, url,
    ST_SetSRID(ST_MakePoint(ST_X(location), CAST(-depth * 1000 AS float8)), 900913)::geometry(Point, 900913) AS depthlocation,
    location
FROM usgs_event;
