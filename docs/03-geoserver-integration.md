# Integración GeoServer WMS

## Arquitectura

```
Frontend (OpenLayers ImageWMS)
        │
        │ GET /geoserver/seismap/wms?...
        ▼
Nginx proxy (/geoserver/ → geoserver:8080/geoserver/)
        │
        ▼
GeoServer (kartoza:2.26.1)
        │ WMS GetMap
        ▼
PostGIS view: eventandaveragemagnitudes
```

## Auto-Configuración al Arranque

`GeoServerAutoConfigService` corre `@EventListener(ApplicationReadyEvent)` y configura via REST API:

1. **Workspace** `seismap`
2. **Datastore** `seismap_postgis` (PostGIS → `postgres:5432/seismap`)
3. **Layer** `eventandaveragemagnitudes` (vista materializada con índice espacial y triggers)
4. **Layer** `eventandaveragemagnitudes_depthlocation` (variante para mapa de profundidad)
5. **7 estilos SLD** (ver tabla abajo)

## Estilos SLD

| Nombre GeoServer | UI Label | Descripción |
|:---|:---|:---|
| `seismap_default` | Por defecto | Círculos azules, tamaño ∝ rankindex |
| `seismap_circles_magnitude` | Círculos — Magnitud | Tamaño variable por magnitud |
| `seismap_circles_depth` | Círculos — Profundidad | Color por rango: rojo (0-30km), violeta (30-70), amarillo (70-300), azul (+300) |
| `seismap_circles_age` | Círculos — Antigüedad | Color uniforme (filtro CQL por cliente) |
| `seismap_points_magnitude` | Puntos — Magnitud | Puntos fijos, color uniforme |
| `seismap_points_depth` | Puntos — Profundidad | Color por rango de profundidad |
| `seismap_points_age` | Puntos — Antigüedad | Color uniforme |

## Vista Materializada

Definida en `V3__materialized_view_geoserver.sql`:
- Columnas: `id`, `location` (Point 900913), `depthlocation`, `depth`, `date`, `rankindex`, `mlmagnitude`, `mbmagnitude`, etc.
- `rankindex` = índice global normalizado 0–1 de magnitud
- Triggers `AFTER INSERT/UPDATE/DELETE` en `event` refreshean la vista

## CQL Filters

`cqlFilter.ts` construye el filtro CQL desde los parámetros del mapa:

```
date >= '2024-01-01T00:00:00' AND date <= '2024-12-31T23:59:59'
AND depth >= 0 AND depth <= 100
AND rankmagnitude >= 0.3 AND rankmagnitude <= 0.8
```

## Decisión: ImageWMS vs TileWMS

Se usa **`ImageWMS`** (no `TileWMS`) porque:
- `TileWMS` genera 12–16 requests simultáneas al hacer zoom → GeoServer devuelve **429**
- `ImageWMS` = 1 request por viewport completo, igual que el proyecto legado con OpenLayers 2
- El proyecto legado (2011) usaba `OpenLayers.Layer.WMS` que también hacía imagen única

## Leyenda

`MapLegend.tsx` muestra la leyenda del estilo activo:
- Endpoint: `GET /api/maps/legend?name={styleName}`
- El backend hace proxy a `GET /geoserver/seismap/wms?REQUEST=GetLegendGraphic&LAYER=...`
- No visible cuando el estilo es `seismap_default`
