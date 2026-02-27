-- V2: Seed reference data (ported from legacy MySQL seed-data.sql)
-- Orden: style primero (application referencia style_id=1), luego application, magnitude_limits, user

-- ─── Styles ──────────────────────────────────────────────────────────────────
INSERT INTO style (id, name, sld, in_application_index, application_id) VALUES
    (1, 'Círculos - Color según magnitud',   'seismap_circles-color-by-magnitude', 0, 1),
    (2, 'Círculos - Color según antigüedad', 'seismap_circles-color-by-age',       1, 1),
    (3, 'Círculos - Color según profundidad','seismap_circles-color-by-depth',      2, 1),
    (4, 'Puntos - Color según magnitud',     'seismap_dots-color-by-magnitude',     3, 1),
    (5, 'Puntos - Color según antigüedad',   'seismap_dots-color-by-age',           4, 1),
    (6, 'Puntos - Color según profundidad',  'seismap_dots-color-by-depth',         5, 1);

-- ─── Application (singleton, id=1) ───────────────────────────────────────────
INSERT INTO application (
    id,
    settings_cache_expiration,
    layer_server_uri,
    legends_directory,
    google_maps_api_key,
    event_map_zoom,
    layer_name,
    depth_layer_name,
    affected_distance_style_name,
    -- Default map
    default_map_name,
    default_map_description,
    default_map_center_longitude,
    default_map_center_latitude,
    default_map_zoom,
    -- Default map dates
    default_map_min_date_type,
    default_map_min_date_relative_amount,
    default_map_min_date_relative_units,
    default_map_min_date,
    default_map_max_date_type,
    default_map_max_date_relative_amount,
    default_map_max_date_relative_units,
    default_map_max_date,
    -- Default map depth
    default_map_min_depth_type,
    default_map_min_depth,
    default_map_max_depth_type,
    default_map_max_depth,
    -- Default map magnitude
    default_map_magnitude_type,
    default_map_list_unmeasured,
    default_map_min_magnitude_type,
    default_map_min_magnitude,
    default_map_max_magnitude_type,
    default_map_max_magnitude,
    -- Default map animation
    default_map_animation_type,
    default_map_animation_step_duration,
    default_map_animation_step_keep,
    default_map_animation_steps,
    default_map_reverse_animation,
    default_map_style_id
) VALUES (
    1,
    5000,
    'http://localhost:8080/geoserver',
    '/opt/seismap/sld/',
    '',
    7,
    'eventandaveragemagnitudes',
    'eventandaveragemagnitudes_depthlocation',
    'seismap_affected-distance',
    -- Default map
    'Nuevo mapa',
    '',
    -68.526111,
    -31.534167,
    10,
    -- Dates
    'RELATIVE',
    1,
    'DAY',
    NOW(),
    'NONE',
    0,
    'HOUR',
    NOW(),
    -- Depth
    'NONE',
    0,
    'NONE',
    0,
    -- Magnitude (stored as enum string in application)
    'ML',
    true,
    'NONE',
    0,
    'NONE',
    10,
    -- Animation
    'NONE',
    5,
    0,
    10,
    false,
    1
);

-- ─── Magnitude Limits ─────────────────────────────────────────────────────────
INSERT INTO magnitude_limits (magnitude_type, min, max) VALUES
    ('MB',   0,  6.5),
    ('MS',   1,  10),
    ('MW',   2,  10),
    ('MBLG', 0,  10),
    ('MC',   1,  12),
    ('ML',   2,  10);

-- ─── Admin User ───────────────────────────────────────────────────────────────
INSERT INTO seismap_user (id, email, name, password_hash, administrator) VALUES
    (1, 'admin@seismap.com', 'Admin', '', true);
