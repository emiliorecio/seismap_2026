# Seismap Migration — Checklist Detallado

## Phase 0: Decisiones ✅
- [x] Mapping library → **OpenLayers**
- [x] GeoServer → **Docker service** (`kartoza/geoserver`)
- [x] Auth → **Diferida** (usuario hardcodeado en V1)
- [x] Admin data-load → **Incluido** en V1
- [x] UI library → **MUI (Material UI)**

---

## Phase 1: Project Scaffolding

### 1.1 Backend (`seismap-backend/`) ✅
- [x] Generar proyecto Spring Boot 3.4 con Maven (web, data-jpa, postgresql, flyway, validation)
- [x] Configurar [pom.xml](file:///home/erecio/Documents/Projects/seismap/pom.xml) con dependencias adicionales (hibernate-spatial, jackson-datatype-jts, springdoc-openapi)
- [x] Crear estructura de paquetes: `controller`, `service`, `repository`, `model/entity`, `dto`, `config`
- [x] Configurar [application.yml](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/resources/application.yml) con datasource placeholder para PostgreSQL/PostGIS

### 1.2 Frontend (`seismap-frontend/`) ✅
- [x] Crear proyecto React + TypeScript con Vite
- [x] Instalar dependencias: MUI, OpenLayers ([ol](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/service/impl/CoordinatesConverter.java#383-389)), React Router, Axios
- [x] Crear estructura de carpetas: `components/`, `pages/`, `services/`, `hooks/`, `types/`
- [x] Configurar proxy de desarrollo en [vite.config.ts](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/vite.config.ts) para apuntar al backend

### 1.3 Docker Compose ✅
- [x] Crear [docker-compose.yml](file:///home/erecio/Documents/Projects/seismap/docker-compose.yml) con servicios: `postgres`, `geoserver`, `backend`, `frontend`
- [x] Configurar volúmenes para datos de PostgreSQL y GeoServer
- [x] Configurar red interna entre servicios
- [x] Verificar que `docker-compose up` levanta postgres y geoserver correctamente

---

## Phase 2: Database & Persistence Layer ✅

### 2.1 Schema y Migraciones ✅
- [x] Extraer esquema SQL del código legacy (entidades Hibernate)
- [x] Crear migración Flyway [V1__init_postgis.sql](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/resources/db/migration/V1__init_postgis.sql)
- [x] Incluir extensión PostGIS (`CREATE EXTENSION IF NOT EXISTS postgis`)

### 2.2 Entidades JPA ✅
- [x] [Application](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/model/entity/Application.java#17-84) + [ApplicationSettings](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/model/entity/ApplicationSettings.java#19-509)
- [x] [Agency](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/java/com/seismap/model/entity/Agency.java#5-31)
- [x] [Category](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/java/com/seismap/model/entity/Category.java#7-46)
- [x] [Event](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/model/entity/Event.java#11-45) (con campo geométrico `Point` para location)
- [x] [EventAndAverageMagnitudes](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/model/entity/EventAndAverageMagnitudes.java#11-93)
- [x] [EventInfo](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/model/entity/EventInfo.java#10-115)
- [x] [Magnitude](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/java/com/seismap/model/entity/Magnitude.java#6-50)
- [x] [MagnitudeDataBounds](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/model/entity/MagnitudeDataBounds.java#11-48)
- [x] [MagnitudeLimits](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/java/com/seismap/model/entity/MagnitudeLimits.java#6-44)
- [x] [SeismapMap](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/java/com/seismap/model/entity/SeismapMap.java#12-372) (configuración de mapa del usuario)
- [x] [Style](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/java/com/seismap/model/entity/Style.java#6-51)
- [x] [DataBounds](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/src/types/map.ts#47-53)
- [x] [User](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/java/com/seismap/model/entity/User.java#7-74)

### 2.3 Repositories ✅
- [x] Crear Spring Data JPA repositories para cada entidad (10 repos)
- [x] Verificar compilación Maven ✅
- [x] Migración [V2__seed_data.sql](file:///home/erecio/Documents/Projects/seismap/seismap-backend/src/main/resources/db/migration/V2__seed_data.sql) — datos de referencia (6 estilos, Application, 6 MagnitudeLimits, usuario Admin)

---

## Phase 3: Backend REST APIs ✅

### 3.1 Application & Config ✅
- [x] `GET /api/application`
- [x] `GET /api/application/settings`

### 3.2 Events ✅
- [x] `GET /api/events/{id}`
- [x] `PUT /api/events/{id}`
- [x] `GET /api/events/data-bounds`
- [x] `GET /api/events/magnitude-limits`

### 3.3 Maps ✅
- [x] `GET /api/maps/default`
- [x] `POST /api/maps`
- [x] `PATCH /api/maps/{id}/name`
- [x] `DELETE /api/maps/{id}`
- [x] `PUT /api/maps/{id}`
- [x] `GET /api/maps/{id}`
- [x] `GET /api/maps?userId={id}`
- [x] `GET /api/maps/legend?name={sld}` ✅ proxy a GeoServer `GetLegendGraphic`
- [ ] `POST /api/events/within` ⬜ pendiente (consulta espacial por polígono)

### 3.4 Styles ✅
- [x] `POST /api/styles`
- [x] `GET /api/styles`

### 3.5 Categories ✅
- [x] `POST /api/categories`
- [x] `GET /api/categories`

### 3.6 Admin (Data Loading) ✅
- [x] `GET /api/admin/data-files`
- [x] `POST /api/admin/load-data-file` ✅ parser Nordic/SEISAN implementado (602 eventos cargados de sample.data)

### 3.7 GeoServer Proxy ✅
- [x] Nginx proxy `/geoserver/` → `geoserver:8080/geoserver/`
- [x] `GeoServerAutoConfigService` — workspace, datastore, layers y 7 estilos SLD al arranque

### 3.8 Documentación ✅
- [x] springdoc-openapi en pom.xml + application.yml

### Compilación ✅
- [x] `mvn compile` exitoso con `JAVA_HOME=~/.sdkman/candidates/java/21.0.2-open`

---

## Phase 4: Frontend Base Setup ✅
- [x] Crear layout principal ([MainLayout](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/src/components/MainLayout.tsx#26-118): sidebar colapsable + área de mapa)
- [x] Inicializar componente OpenLayers con capas base OSM ([SeismapMapView.tsx](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/src/components/SeismapMapView.tsx))
- [x] Configurar tema MUI dark (navy + azul sísmico)
- [x] Configurar React Router (`/` → mapa, `/admin` → admin)
- [x] Crear servicio HTTP base con Axios + servicios por dominio ([seismap.ts](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/src/services/seismap.ts))
- [x] Tipos TypeScript para todas las entidades ([types/map.ts](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/src/types/map.ts))
- [x] `npm run build` exitoso (2.8s, sin errores TS)

---

## Phase 5: Frontend Features ✅

### 5.1 Mapa Principal ✅
- [x] Panel de parámetros ([MapControlsPanel](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/src/components/MapControlsPanel.tsx#22-226): filtros de fecha, profundidad, magnitud)
- [x] Controles de animación
- [x] Selector de estilos (`MapControlsPanel` sección "Vista" — 7 estilos)
- [x] Capa WMS GeoServer (`ImageWMS` en `SeismapMapView.tsx`, CQL dinámico)
- [x] Leyenda del mapa (`MapLegend.tsx` flotante sobre el mapa)

### 5.2 Gestión de Mapas ✅
- [x] Guardar mapa (botón en [SavedMapsPanel](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/src/components/SavedMapsPanel.tsx#26-163))
- [x] Renombrar mapa (dialog inline)
- [x] Cargar mapa existente (lista clickeable)
- [x] Eliminar mapa

### 5.3 Eventos Sísmicos
- [ ] Click en mapa → mostrar eventos de la ubicación (Phase siguiente)
- [ ] Diálogo de detalle de evento
- [ ] Mapa secundario de evento

### 5.4 Profundidad
- [ ] Herramienta de polígono + mapa de profundidad (Phase siguiente)

### 5.5 Admin ✅
- [x] Página de listado de archivos [.data](file:///home/erecio/Documents/Projects/seismap/src/test/resources/datafiles/sample.data)
- [x] Botón de carga de archivo → llamada a API

### Build ✅
- [x] `npm run build` exitoso (3.13s, sin errores TS)

---

## Phase 6: Docker & Delivery ✅

- [x] Dockerfile Backend (multi-stage Maven 3.9 + JRE 21 Alpine)
- [x] Dockerfile Frontend (Node 20 Alpine build + Nginx 1.27 Alpine)
- [x] [nginx.conf](file:///home/erecio/Documents/Projects/seismap/seismap-frontend/nginx.conf) (SPA pushState, proxy /api/ y /layerServer/, gzip, cache assets)
- [x] [docker-compose.yml](file:///home/erecio/Documents/Projects/seismap/docker-compose.yml) actualizado con los 4 servicios (health checks)
- [x] `spring-boot-starter-actuator` agregado para health checks Docker
- [x] Carpeta [data/](file:///home/erecio/Documents/Projects/seismap/src/test/resources/datafiles/sample.data) creada para archivos .data
- [x] [README.md](file:///home/erecio/Documents/Projects/seismap/README.md) con instrucciones de setup, desarrollo local y variables de entorno
- [x] Test end-to-end: `docker compose up` ✅ (4/4 servicios healthy)


