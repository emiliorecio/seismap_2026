# Funcionalidades Pendientes

## 1. Selección por Polígono

Dibujar un polígono cerrado en el mapa y obtener los eventos sísmicos dentro del área.

### Referencia Legado

El proyecto original usaba:
```javascript
this.depthPolygonControl = new OpenLayers.Control.DrawFeature(
    layer, OpenLayers.Handler.Polygon,
    { featureAdded: function(vector) { self.showDepthWindow(vector); } }
);
// CQL filter construido:
cqlFilter += 'WITHIN(location, POLYGON((' + coords + ')))';
```

### Plan de Implementación

**Backend**

- `EventRepository`: query nativa `ST_Within(e.location, ST_GeomFromText(:wkt, 900913))`
- `EventService`: método `findWithinPolygon(String wkt)`
- `EventController`: `POST /api/events/within` → `{ "wkt": "POLYGON(...)" }`
- `EventSummaryDto`: `id`, `date`, `depth`, `latitude`, `longitude`, `name`, `reference`, `rankMagnitude`

**Frontend**

- `SeismapMapView.tsx`: agregar `VectorLayer` + `Draw` interaction (tipo `Polygon`)
- Botón en AppBar para activar/desactivar modo dibujo
- `EventsWithinDialog.tsx`: dialog con tabla MUI (fecha, prof., magnitud, referencia)
- `seismap.ts`: `eventService.findWithin(wkt)`
- Las coordenadas van en EPSG:3857 (sistem OpenLayers) → convertir a WKT para la API

**Conversión de coordenadas**:
```typescript
import { toLonLat } from 'ol/proj';
// Los vértices del polígono dibujado están en EPSG:3857
// El CQL de GeoServer necesita EPSG:900913 (= 3857)
// → usar las coordenadas directamente sin reproyección
const coords = polygon.getCoordinates()[0]
    .map(c => `${c[0]} ${c[1]}`).join(', ');
const wkt = `POLYGON((${coords}))`;
```

---

## 2. Click en Punto → Detalle de Evento

Click en un punto del mapa → mostrar diálogo con detalle del evento.

### Referencia Legado

```javascript
// WMS GetFeatureInfo
this.registerGetFeaturesControl(this.map, this.getFeatures);
// Luego:
seismap.ui.getFeatures = function(e) {
    // Carga lista de eventos en la ubicación
};
```

### Plan de Implementación

- Agregar `singleclick` event listener al mapa OpenLayers
- Llamar a `GetFeatureInfo` via WMS: `REQUEST=GetFeatureInfo&INFO_FORMAT=application/json&I=...&J=...`
- O bien: `GET /api/events/{id}` si el ID es accesible desde GetFeatureInfo
- `EventDetailDialog.tsx`: nombre, referencia, fecha, profundidad, magnitud, notas

---

## 3. Mapa de Profundidad

Vista de corte transversal mostrando sismos en profundidad dentro de un polígono.

### Referencia Legado

- Usuario dibujaba un polígono → se abría ventana con mapa de profundidad
- Capa: `eventandaveragemagnitudes_depthlocation` (campo `depthlocation`)
- El eje Y del mapa era la profundidad

### Plan de Implementación

- Reutilizar el polígono dibujado en la feature #1
- Mostrar segunda capa WMS con `LAYERS=seismap:eventandaveragemagnitudes_depthlocation`
- Consulta SQL con `WITHIN(depthlocation, POLYGON(...))`

---

## 4. Exportación KML / CSV

Exportar los eventos visibles (con los filtros activos) a KML o CSV.

### Plan de Implementación

- `GET /api/events/export?format=kml&cql=...` o `format=csv`
- Backend: usar los mismos filtros CQL para traer los eventos y formatear la respuesta
- Frontend: botón en el panel de controles → descarga directa
