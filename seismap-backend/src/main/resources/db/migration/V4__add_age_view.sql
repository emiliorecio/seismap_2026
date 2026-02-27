CREATE OR REPLACE VIEW eventandaveragemagnitudes_live AS
SELECT *, date_part('epoch', now() - date) AS age
FROM eventandaveragemagnitudes;
