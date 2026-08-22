-- V7: Table for historical earthquakes imported from the USGS catalog.
-- Kept separate from `event` (the Nordic/SEISAN-parsed local catalog) so the
-- two sources never mix, can be toggled/removed independently as their own
-- map layer, and importing again is a plain upsert keyed by the USGS event id.
CREATE TABLE usgs_event (
    id VARCHAR(64) PRIMARY KEY,
    location geometry(Point, 900913) NOT NULL,
    depth REAL NOT NULL,
    date TIMESTAMP NOT NULL,
    magnitude REAL,
    magnitude_type VARCHAR(10),
    place VARCHAR(255),
    url VARCHAR(500)
);

CREATE INDEX idx_usgs_event_location ON usgs_event USING GIST (location);
CREATE INDEX idx_usgs_event_date ON usgs_event (date);
