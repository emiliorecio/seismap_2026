# Funcionalidades Pendientes

## Implementadas (movidas desde este doc)

Las siguientes funcionalidades, antes listadas como pendientes acá, ya están implementadas en el código actual:

- **Selección por polígono** — `EventController.POST /api/events/within`, `EventRepository#findWithinPolygon` (`ST_Within`), dibujo de polígono en `SeismapMapView.tsx`, resultados en `EventsWithinDialog.tsx`.
- **Click en punto → detalle de evento** — listener `singleclick` en `SeismapMapView.tsx` (GetFeatureInfo) + `EventDialog.tsx`.
- **Mapa de profundidad (corte transversal)** — tab "Corte Transversal" en `EventsWithinDialog.tsx`, consume la capa WMS `eventandaveragemagnitudes_depthlocation` (vista materializada creada en `V3__materialized_view_geoserver.sql`, columna `depthlocation` con Y = profundidad negativa).

Ver estado real en `docs/02-task-checklist.md`.

---

## 1. Exportación KML / CSV

Exportar los eventos visibles (con los filtros activos) a KML o CSV.

### Plan de Implementación

- `GET /api/events/export?format=kml&cql=...` o `format=csv`
- Backend: usar los mismos filtros CQL para traer los eventos y formatear la respuesta
- Frontend: botón en el panel de controles → descarga directa
