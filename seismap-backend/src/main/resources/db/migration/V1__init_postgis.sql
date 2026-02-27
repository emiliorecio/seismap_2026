-- V1: Seismap Initial Schema
CREATE EXTENSION IF NOT EXISTS postgis;

-- Agency
CREATE TABLE agency (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE
);

-- User (SeismapUser to avoid reserved word)
CREATE TABLE seismap_user (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    administrator BOOLEAN NOT NULL DEFAULT FALSE
);

-- Style
CREATE TABLE style (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT,
    in_application_index INT,
    sld VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);

-- Style variables (ElementCollection)
CREATE TABLE style_variable (
    style_id BIGINT NOT NULL REFERENCES style(id),
    name VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    PRIMARY KEY (style_id, name)
);

-- Category
CREATE TABLE category (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT,
    in_application_index INT,
    name VARCHAR(255) NOT NULL
);

-- Map (seismic map configuration)
CREATE TABLE map (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES seismap_user(id),
    category_id BIGINT REFERENCES category(id),
    in_category_index INT,
    in_user_index INT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    center geometry(Point, 900913) NOT NULL,
    zoom INT NOT NULL,
    min_date_type VARCHAR(20) NOT NULL,
    min_date_relative_amount REAL NOT NULL,
    min_date_relative_units VARCHAR(20) NOT NULL,
    min_date TIMESTAMP NOT NULL,
    max_date_type VARCHAR(20) NOT NULL,
    max_date_relative_amount REAL NOT NULL,
    max_date_relative_units VARCHAR(20) NOT NULL,
    max_date TIMESTAMP NOT NULL,
    min_depth_type VARCHAR(20) NOT NULL,
    min_depth REAL,
    max_depth_type VARCHAR(20) NOT NULL,
    max_depth REAL,
    magnitude_type INTEGER NOT NULL DEFAULT 0,
    min_magnitude_type VARCHAR(20) NOT NULL,
    min_magnitude REAL,
    max_magnitude_type VARCHAR(20) NOT NULL,
    max_magnitude REAL,
    list_unmeasured BOOLEAN NOT NULL,
    animation_type VARCHAR(20) NOT NULL,
    animation_step_keep REAL NOT NULL,
    animation_steps INT NOT NULL,
    animation_step_duration REAL NOT NULL,
    reverse_animation BOOLEAN NOT NULL,
    style_id BIGINT NOT NULL REFERENCES style(id)
);

-- Event
CREATE TABLE event (
    id BIGSERIAL PRIMARY KEY,
    location geometry(Point, 900913) NOT NULL,
    depth REAL NOT NULL,
    date TIMESTAMP NOT NULL,
    name VARCHAR(255),
    notes TEXT,
    reference VARCHAR(255),
    perceived_distance INT,
    damaged_distance INT
);

-- Magnitude
CREATE TABLE magnitude (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id),
    type VARCHAR(10) NOT NULL,
    value REAL NOT NULL,
    reporting_agency_id BIGINT NOT NULL REFERENCES agency(id)
);

-- Application (singleton config)
CREATE TABLE application (
    id BIGINT PRIMARY KEY,
    settings_cache_expiration BIGINT NOT NULL,
    layer_server_uri VARCHAR(255) NOT NULL,
    google_maps_api_key VARCHAR(255) NOT NULL,
    event_map_zoom INT NOT NULL,
    layer_name VARCHAR(255) NOT NULL,
    depth_layer_name VARCHAR(255) NOT NULL,
    affected_distance_style_name VARCHAR(255) NOT NULL,
    default_map_name VARCHAR(255) NOT NULL,
    default_map_description VARCHAR(255) NOT NULL,
    default_map_center_longitude DOUBLE PRECISION NOT NULL,
    default_map_center_latitude DOUBLE PRECISION NOT NULL,
    default_map_zoom INT NOT NULL,
    default_map_min_date_type VARCHAR(20) NOT NULL,
    default_map_min_date_relative_amount REAL NOT NULL,
    default_map_min_date_relative_units VARCHAR(20) NOT NULL,
    default_map_min_date TIMESTAMP,
    default_map_max_date_type VARCHAR(20) NOT NULL,
    default_map_max_date_relative_amount REAL NOT NULL,
    default_map_max_date_relative_units VARCHAR(20) NOT NULL,
    default_map_max_date TIMESTAMP,
    default_map_min_depth_type VARCHAR(20) NOT NULL,
    default_map_min_depth REAL,
    default_map_max_depth_type VARCHAR(20) NOT NULL,
    default_map_max_depth REAL,
    default_map_magnitude_type VARCHAR(20) NOT NULL,
    default_map_min_magnitude_type VARCHAR(20) NOT NULL,
    default_map_min_magnitude REAL,
    default_map_max_magnitude_type VARCHAR(20) NOT NULL,
    default_map_max_magnitude REAL,
    default_map_list_unmeasured BOOLEAN NOT NULL,
    default_map_animation_type VARCHAR(20) NOT NULL,
    default_map_animation_step_keep REAL NOT NULL,
    default_map_animation_steps INT NOT NULL,
    default_map_animation_step_duration REAL NOT NULL,
    default_map_reverse_animation BOOLEAN NOT NULL,
    default_map_style_id BIGINT NOT NULL REFERENCES style(id),
    legends_directory VARCHAR(255) NOT NULL
);

-- DataBounds (calculated data bounds for events)
CREATE TABLE data_bounds (
    id BIGINT PRIMARY KEY,
    min_date TIMESTAMP,
    max_date TIMESTAMP,
    min_depth REAL,
    max_depth REAL
);

-- MagnitudeDataBounds
CREATE TABLE magnitude_data_bounds (
    magnitude_type VARCHAR(10) PRIMARY KEY,
    data_bound_id BIGINT REFERENCES data_bounds(id),
    min REAL,
    max REAL
);

-- MagnitudeLimits
CREATE TABLE magnitude_limits (
    magnitude_type VARCHAR(10) PRIMARY KEY,
    min REAL NOT NULL,
    max REAL NOT NULL
);

-- View: EventAndAverageMagnitudes (read-only, created by DB)
CREATE OR REPLACE VIEW eventandaveragemagnitudes AS
SELECT
    e.id,
    e.location,
    e.depth,
    e.date,
    e.name,
    e.notes,
    e.reference,
    e.perceived_distance,
    e.damaged_distance,
    AVG(CASE WHEN m.type = 'ML' THEN m.value END) AS ml_magnitude,
    AVG(CASE WHEN m.type = 'MB' THEN m.value END) AS mb_magnitude,
    AVG(CASE WHEN m.type = 'MS' THEN m.value END) AS ms_magnitude,
    AVG(CASE WHEN m.type = 'MW' THEN m.value END) AS mw_magnitude,
    AVG(CASE WHEN m.type = 'MBLG' THEN m.value END) AS mblg_magnitude,
    AVG(CASE WHEN m.type = 'MC' THEN m.value END) AS mc_magnitude,
    COALESCE(
        AVG(CASE WHEN m.type = 'MW' THEN m.value END),
        AVG(CASE WHEN m.type = 'MS' THEN m.value END),
        AVG(CASE WHEN m.type = 'MB' THEN m.value END),
        AVG(CASE WHEN m.type = 'ML' THEN m.value END),
        AVG(CASE WHEN m.type = 'MBLG' THEN m.value END),
        AVG(CASE WHEN m.type = 'MC' THEN m.value END)
    ) AS rank_magnitude
FROM event e
LEFT JOIN magnitude m ON m.event_id = e.id
GROUP BY e.id;

-- Spatial indexes
CREATE INDEX idx_event_location ON event USING GIST (location);
CREATE INDEX idx_map_center ON map USING GIST (center);
CREATE INDEX idx_event_date ON event (date);
CREATE INDEX idx_event_depth ON event (depth);
CREATE INDEX idx_magnitude_type_value ON magnitude (type, value);
