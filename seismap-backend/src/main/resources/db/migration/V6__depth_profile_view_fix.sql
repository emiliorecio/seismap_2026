-- V6: V5 dropped `location` from the depth-profile view to make `depthlocation`
-- the unambiguous default geometry, but the frontend's polygon selection
-- still filters this layer with CQL_FILTER=WITHIN(location, ...), which broke
-- GetMap rendering entirely ("Property 'location' could not be found").
--
-- Both columns are needed: `location` for the polygon CQL filter, and
-- `depthlocation` as GeoServer's default/hit-test geometry. GeoServer treats
-- the first-declared geometry column as the default, so `depthlocation` is
-- placed before `location` here.
--
-- CREATE OR REPLACE VIEW cannot reorder columns, so the view is recreated.
DROP VIEW eventandaveragemagnitudes_depth_live;

CREATE VIEW eventandaveragemagnitudes_depth_live AS
SELECT
    id, date, depth, name, notes, reference, perceived_distance, damaged_distance,
    mlmagnitude, mbmagnitude, msmagnitude, mwmagnitude, mblgmagnitude, mcmagnitude,
    minmlmagnitude, maxmlmagnitude, minmbmagnitude, maxmbmagnitude, minmsmagnitude, maxmsmagnitude,
    minmwmagnitude, maxmwmagnitude, minmblgmagnitude, maxmblgmagnitude, minmcmagnitude, maxmcmagnitude,
    mlindex, mbindex, msindex, mwindex, mblgindex, mcindex, rankmagnitude, rankindex, age,
    depthlocation, location
FROM eventandaveragemagnitudes_live;
