-- V9: Adds an `age` column (seconds since the event) to the USGS layer, the
-- same way V4 did for the local catalog, so USGS can offer an "age" style
-- matching the local catalog's Vista options.
CREATE OR REPLACE VIEW usgs_event_live AS
SELECT *, date_part('epoch', now() - date) AS age
FROM usgs_event;
