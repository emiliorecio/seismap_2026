-- V3: Materialized view for GeoServer WMS layers
-- Replaces the simplified V1 view with the full legacy version:
-- normalized magnitude indices, depthlocation, triggers for auto-refresh

-- Drop the simplified view from V1
DROP VIEW IF EXISTS eventandaveragemagnitudes;

-- ─── Unmaterialized base view ─────────────────────────────────────────────────
CREATE VIEW eventandaveragemagnitudes_unmaterialized AS
SELECT
    event.id,
    event.date,
    event.depth,
    event.name,
    event.notes,
    event.reference,
    event.perceived_distance,
    event.damaged_distance,
    -- Average magnitudes by type
    avg(mlmag.value)::real   AS mlmagnitude,
    avg(mbmag.value)::real   AS mbmagnitude,
    avg(msmag.value)::real   AS msmagnitude,
    avg(mwmag.value)::real   AS mwmagnitude,
    avg(mblgmag.value)::real AS mblgmagnitude,
    avg(mcmag.value)::real   AS mcmagnitude,
    -- Magnitude limits (for normalization)
    mllim.min AS minmlmagnitude, mllim.max AS maxmlmagnitude,
    mblim.min AS minmbmagnitude, mblim.max AS maxmbmagnitude,
    mslim.min AS minmsmagnitude, mslim.max AS maxmsmagnitude,
    mwlim.min AS minmwmagnitude, mwlim.max AS maxmwmagnitude,
    mblglim.min AS minmblgmagnitude, mblglim.max AS maxmblgmagnitude,
    mclim.min AS minmcmagnitude, mclim.max AS maxmcmagnitude,
    -- Normalized indices (0 to 1)
    CASE WHEN mllim.max <> mllim.min THEN ((avg(mlmag.value) - mllim.min) / (mllim.max - mllim.min))::real END AS mlindex,
    CASE WHEN mblim.max <> mblim.min THEN ((avg(mbmag.value) - mblim.min) / (mblim.max - mblim.min))::real END AS mbindex,
    CASE WHEN mslim.max <> mslim.min THEN ((avg(msmag.value) - mslim.min) / (mslim.max - mslim.min))::real END AS msindex,
    CASE WHEN mwlim.max <> mwlim.min THEN ((avg(mwmag.value) - mwlim.min) / (mwlim.max - mwlim.min))::real END AS mwindex,
    CASE WHEN mblglim.max <> mblglim.min THEN ((avg(mblgmag.value) - mblglim.min) / (mblglim.max - mblglim.min))::real END AS mblgindex,
    CASE WHEN mclim.max <> mclim.min THEN ((avg(mcmag.value) - mclim.min) / (mclim.max - mclim.min))::real END AS mcindex,
    -- Rank magnitude (greatest normalized * 10, range 0-10)
    (GREATEST(
        CASE WHEN mllim.max <> mllim.min THEN (avg(mlmag.value) - mllim.min) / (mllim.max - mllim.min) END,
        CASE WHEN mblim.max <> mblim.min THEN (avg(mbmag.value) - mblim.min) / (mblim.max - mblim.min) END,
        CASE WHEN mslim.max <> mslim.min THEN (avg(msmag.value) - mslim.min) / (mslim.max - mslim.min) END,
        CASE WHEN mwlim.max <> mwlim.min THEN (avg(mwmag.value) - mwlim.min) / (mwlim.max - mwlim.min) END,
        CASE WHEN mblglim.max <> mblglim.min THEN (avg(mblgmag.value) - mblglim.min) / (mblglim.max - mblglim.min) END,
        CASE WHEN mclim.max <> mclim.min THEN (avg(mcmag.value) - mclim.min) / (mclim.max - mclim.min) END
    ) * 10)::real AS rankmagnitude,
    -- Rank index (greatest normalized, range 0-1)
    GREATEST(
        CASE WHEN mllim.max <> mllim.min THEN (avg(mlmag.value) - mllim.min) / (mllim.max - mllim.min) END,
        CASE WHEN mblim.max <> mblim.min THEN (avg(mbmag.value) - mblim.min) / (mblim.max - mblim.min) END,
        CASE WHEN mslim.max <> mslim.min THEN (avg(msmag.value) - mslim.min) / (mslim.max - mslim.min) END,
        CASE WHEN mwlim.max <> mwlim.min THEN (avg(mwmag.value) - mwlim.min) / (mwlim.max - mwlim.min) END,
        CASE WHEN mblglim.max <> mblglim.min THEN (avg(mblgmag.value) - mblglim.min) / (mblglim.max - mblglim.min) END,
        CASE WHEN mclim.max <> mclim.min THEN (avg(mcmag.value) - mclim.min) / (mclim.max - mclim.min) END
    )::real AS rankindex,
    -- Location (EPSG:900913)
    event.location::geometry(Point, 900913) AS location,
    -- Depth location: same X as event, Y = negative depth in meters
    ST_SetSRID(ST_MakePoint(ST_X(event.location), CAST(-event.depth * 1000 AS float8)), 900913)::geometry(Point, 900913) AS depthlocation
FROM event
    LEFT JOIN magnitude mlmag   ON event.id = mlmag.event_id   AND mlmag.type   = 'ML'
    LEFT JOIN magnitude mbmag   ON event.id = mbmag.event_id   AND mbmag.type   = 'MB'
    LEFT JOIN magnitude msmag   ON event.id = msmag.event_id   AND msmag.type   = 'MS'
    LEFT JOIN magnitude mwmag   ON event.id = mwmag.event_id   AND mwmag.type   = 'MW'
    LEFT JOIN magnitude mblgmag ON event.id = mblgmag.event_id AND mblgmag.type = 'MBLG'
    LEFT JOIN magnitude mcmag   ON event.id = mcmag.event_id   AND mcmag.type   = 'MC'
    LEFT JOIN magnitude_limits mllim   ON mllim.magnitude_type   = 'ML'
    LEFT JOIN magnitude_limits mblim   ON mblim.magnitude_type   = 'MB'
    LEFT JOIN magnitude_limits mslim   ON mslim.magnitude_type   = 'MS'
    LEFT JOIN magnitude_limits mwlim   ON mwlim.magnitude_type   = 'MW'
    LEFT JOIN magnitude_limits mblglim ON mblglim.magnitude_type = 'MBLG'
    LEFT JOIN magnitude_limits mclim   ON mclim.magnitude_type   = 'MC'
GROUP BY
    event.id, event.date, event.depth, event.name, event.notes, event.reference,
    event.perceived_distance, event.damaged_distance, event.location,
    mllim.min, mllim.max, mblim.min, mblim.max,
    mslim.min, mslim.max, mwlim.min, mwlim.max,
    mblglim.min, mblglim.max, mclim.min, mclim.max;

-- ─── Materialized table ───────────────────────────────────────────────────────
CREATE TABLE eventandaveragemagnitudes AS
    SELECT * FROM eventandaveragemagnitudes_unmaterialized;

ALTER TABLE eventandaveragemagnitudes ADD PRIMARY KEY (id);
CREATE INDEX idx_eaam_location ON eventandaveragemagnitudes USING GIST (location);
CREATE INDEX idx_eaam_depthlocation ON eventandaveragemagnitudes USING GIST (depthlocation);
CREATE INDEX idx_eaam_date ON eventandaveragemagnitudes (date);

-- ─── Refresh functions ────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION eventandaveragemagnitudes_refresh_row(eventid BIGINT) RETURNS VOID
SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM eventandaveragemagnitudes WHERE id = eventid;
    INSERT INTO eventandaveragemagnitudes
        SELECT * FROM eventandaveragemagnitudes_unmaterialized WHERE id = eventid;
END;
$$;

CREATE OR REPLACE FUNCTION eventandaveragemagnitudes_refresh_table() RETURNS VOID
SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN
    TRUNCATE TABLE eventandaveragemagnitudes;
    INSERT INTO eventandaveragemagnitudes
        SELECT * FROM eventandaveragemagnitudes_unmaterialized;
END;
$$;

-- ─── Triggers on event table ──────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION event_it() RETURNS TRIGGER SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN PERFORM eventandaveragemagnitudes_refresh_row(NEW.id); RETURN NULL; END; $$;

CREATE OR REPLACE FUNCTION event_ut() RETURNS TRIGGER SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.id = NEW.id THEN
        PERFORM eventandaveragemagnitudes_refresh_row(NEW.id);
    ELSE
        PERFORM eventandaveragemagnitudes_refresh_row(OLD.id);
        PERFORM eventandaveragemagnitudes_refresh_row(NEW.id);
    END IF;
    RETURN NULL;
END; $$;

CREATE OR REPLACE FUNCTION event_dt() RETURNS TRIGGER SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN PERFORM eventandaveragemagnitudes_refresh_row(OLD.id); RETURN NULL; END; $$;

CREATE TRIGGER event_it AFTER INSERT ON event FOR EACH ROW EXECUTE PROCEDURE event_it();
CREATE TRIGGER event_ut AFTER UPDATE ON event FOR EACH ROW EXECUTE PROCEDURE event_ut();
CREATE TRIGGER event_dt AFTER DELETE ON event FOR EACH ROW EXECUTE PROCEDURE event_dt();

-- ─── Triggers on magnitude table ──────────────────────────────────────────────
CREATE OR REPLACE FUNCTION magnitude_it() RETURNS TRIGGER SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN PERFORM eventandaveragemagnitudes_refresh_row(NEW.event_id); RETURN NULL; END; $$;

CREATE OR REPLACE FUNCTION magnitude_ut() RETURNS TRIGGER SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.event_id = NEW.event_id THEN
        PERFORM eventandaveragemagnitudes_refresh_row(NEW.event_id);
    ELSE
        PERFORM eventandaveragemagnitudes_refresh_row(OLD.event_id);
        PERFORM eventandaveragemagnitudes_refresh_row(NEW.event_id);
    END IF;
    RETURN NULL;
END; $$;

CREATE OR REPLACE FUNCTION magnitude_dt() RETURNS TRIGGER SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN PERFORM eventandaveragemagnitudes_refresh_row(OLD.event_id); RETURN NULL; END; $$;

CREATE TRIGGER magnitude_it AFTER INSERT ON magnitude FOR EACH ROW EXECUTE PROCEDURE magnitude_it();
CREATE TRIGGER magnitude_ut AFTER UPDATE ON magnitude FOR EACH ROW EXECUTE PROCEDURE magnitude_ut();
CREATE TRIGGER magnitude_dt AFTER DELETE ON magnitude FOR EACH ROW EXECUTE PROCEDURE magnitude_dt();

-- ─── Trigger on magnitude_limits ──────────────────────────────────────────────
CREATE OR REPLACE FUNCTION magnitudelimits_ct() RETURNS TRIGGER SECURITY DEFINER LANGUAGE plpgsql AS $$
BEGIN PERFORM eventandaveragemagnitudes_refresh_table(); RETURN NULL; END; $$;

CREATE TRIGGER magnitudelimits_ct AFTER INSERT OR UPDATE OR DELETE ON magnitude_limits
    FOR EACH STATEMENT EXECUTE PROCEDURE magnitudelimits_ct();

-- ─── DataBounds view ──────────────────────────────────────────────────────────
DROP VIEW IF EXISTS databounds;
CREATE VIEW databounds AS
    SELECT 1::bigint AS id,
           min(depth) AS mindepth, max(depth) AS maxdepth,
           min(date) AS mindate, max(date) AS maxdate
    FROM eventandaveragemagnitudes;

-- ─── MagnitudeDataBounds view ─────────────────────────────────────────────────
DROP VIEW IF EXISTS magnitudedatabounds;
CREATE VIEW magnitudedatabounds AS
    SELECT 1::bigint as databound_id, 'RANK' as magnitudetype, min(rankmagnitude) AS min, max(rankmagnitude) AS max FROM eventandaveragemagnitudes
    UNION SELECT 1, 'ML', min(mlmagnitude), max(mlmagnitude) FROM eventandaveragemagnitudes
    UNION SELECT 1, 'MB', min(mbmagnitude), max(mbmagnitude) FROM eventandaveragemagnitudes
    UNION SELECT 1, 'MS', min(msmagnitude), max(msmagnitude) FROM eventandaveragemagnitudes
    UNION SELECT 1, 'MW', min(mwmagnitude), max(mwmagnitude) FROM eventandaveragemagnitudes
    UNION SELECT 1, 'MBLG', min(mblgmagnitude), max(mblgmagnitude) FROM eventandaveragemagnitudes
    UNION SELECT 1, 'MC', min(mcmagnitude), max(mcmagnitude) FROM eventandaveragemagnitudes;
