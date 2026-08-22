-- V5: Dedicated view for the depth-profile GeoServer layer.
--
-- eventandaveragemagnitudes_live has two geometry columns (location,
-- depthlocation). GeoServer always picks the first declared geometry column
-- as a feature type's default geometry for spatial operations like
-- GetFeatureInfo, regardless of which geometry the SLD renders with — so the
-- depth-profile layer's click hit-testing was silently checking against
-- `location` instead of `depthlocation` and never matching.
--
-- This view exposes depthlocation as the only geometry column, removing the
-- ambiguity.
CREATE OR REPLACE VIEW eventandaveragemagnitudes_depth_live AS
SELECT
    id, date, depth, name, notes, reference, perceived_distance, damaged_distance,
    mlmagnitude, mbmagnitude, msmagnitude, mwmagnitude, mblgmagnitude, mcmagnitude,
    minmlmagnitude, maxmlmagnitude, minmbmagnitude, maxmbmagnitude, minmsmagnitude, maxmsmagnitude,
    minmwmagnitude, maxmwmagnitude, minmblgmagnitude, maxmblgmagnitude, minmcmagnitude, maxmcmagnitude,
    mlindex, mbindex, msindex, mwindex, mblgindex, mcindex, rankmagnitude, rankindex, age,
    depthlocation
FROM eventandaveragemagnitudes_live;
