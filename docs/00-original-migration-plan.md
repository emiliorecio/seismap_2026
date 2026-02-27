# Seismap Migration Plan

Modernize the 2011 Seismap application from Java 1.5 / Spring 3.2 / ExtJS to Java 21 / Spring Boot 3 / React + OpenLayers, containerized with Docker Compose.

---

## Current Technology Stack (Legacy)

### Backend
| Component | Version |
|:---|:---|
| Java | 1.5 |
| Spring Framework | 3.2.4 (XML bean config, no annotations for DI) |
| Hibernate + Hibernate Spatial | 4.1.9 / 4.0 |
| PostgreSQL + PostGIS | 9.1 / 1.3.3 |
| Servlet API | 2.5 (WAR deployed to Tomcat/Jetty) |
| GeoTools | 13.0 |
| JTS (Geometry) | vividsolutions (`com.vividsolutions.jts`) |
| Jackson JSON | 1.6.1 (Codehaus, pre-FasterXML) |
| Build | Maven |

### Frontend
| Component | Details |
|:---|:---|
| Templating | JSPs ([map.jsp](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/jsp/map.jsp), [home.jsp](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/jsp/home.jsp), [admin.jsp](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/jsp/admin.jsp), [data-files-list.jsp](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/jsp/data-files-list.jsp)) |
| UI Framework | ExtJS 3.x (Ext Designer generated) |
| Mapping | OpenLayers classic + GeoExt + Google Maps API base layers |
| State | ExtJS Stores (`EventStore`, `LocationEventStore`, `StyleStore`, etc.) |

### Authentication
> [!WARNING]
> **No real authentication exists.** `SeismapController.getActorCredentials()` returns a hardcoded user: `new ActorCredentialsDto(1L, "")`. All controllers inherit this.

### Layer Server (GeoServer Proxy)
> [!IMPORTANT]
> `PageController.layerServer()` is a **reverse proxy** that forwards requests to an external GeoServer instance (URL stored in `ApplicationSettings.layerServerUri`). The frontend hits `/layerServer/*` and the backend proxies those to GeoServer for WMS/WFS tile rendering. **GeoServer is a separate, external dependency** — not part of this codebase.

---

## Domain Model Inventory

### Entities (12)
[Application](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/ApplicationController.java#15-39), `ApplicationSettings`, `Agency`, [Category](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/CategoryController.java#15-39), [DataBounds](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/EventController.java#36-42), [Event](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/resources/js/seismap.js#400-439), `EventAndAverageMagnitudes`, [EventInfo](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/resources/js/seismap.js#981-1003), [Magnitude](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/EventController.java#43-49), `MagnitudeDataBounds`, [MagnitudeLimits](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/EventController.java#43-49), [Map](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/resources/js/seismap.js#1003-1078), [Style](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/StyleController.java#15-38), [User](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#85-91) (+ `Identifiable` base, `ListManager` utility)

### Repositories (9)
`AgencyRepository`, `ApplicationRepository`, `CategoryRepository`, `DataBoundsRepository`, `EventRepository`, `EventAndAverageMagnitudesRepository`, `MagnitudeLimitsRepository`, `MapRepository`, `StyleRepository`, `UserRepository`

### Services (7)
| Service | Key Operations |
|:---|:---|
| `ApplicationService` | [get](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/ApplicationController.java#25-31), [getSettings](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/ApplicationController.java#32-38) |
| `CategoryService` | [create](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/StyleController.java#25-31), [list](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/StyleController.java#32-37) |
| `EventService` | [get](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/ApplicationController.java#25-31), [modify](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#73-78), [getDataBounds](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/EventController.java#36-42), [getMagnitudeLimits](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/EventController.java#43-49) |
| `MapService` | [getDefault](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#48-54), [create](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/StyleController.java#25-31), [rename](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#61-66), [delete](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#67-72), [modify](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#73-78), [get](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/ApplicationController.java#25-31), [listByUser](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#85-91), [getLegend](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#92-107) |
| `StyleService` | [create](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/StyleController.java#25-31), [list](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/StyleController.java#32-37) |
| `UserService` | [create](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/StyleController.java#25-31) |
| `DataLoadService` | [load](file:///home/erecio/Documents/Projects/seismap/src/main/webapp/resources/js/seismap.js#1131-1153) (bulk import from `.data` files) |

### Full API Endpoint Inventory (Legacy → New)
| Legacy Route | Method | New REST Endpoint |
|:---|:---|:---|
| `action/application/get` | POST | `GET /api/application` |
| `action/application/getSettings` | POST | `GET /api/application/settings` |
| `action/category/create` | POST | `POST /api/categories` |
| `action/category/list` | POST | `GET /api/categories` |
| `action/event/get` | POST | `GET /api/events/{id}` |
| `action/event/modify` | POST | `PUT /api/events/{id}` |
| `action/event/getDataBounds` | POST | `GET /api/events/data-bounds` |
| `action/event/getMagnitudeLimits` | POST | `GET /api/events/magnitude-limits` |
| `action/map/getDefault` | POST | `GET /api/maps/default` |
| `action/map/create` | POST | `POST /api/maps` |
| `action/map/rename` | POST | `PATCH /api/maps/{id}/name` |
| `action/map/delete` | POST | `DELETE /api/maps/{id}` |
| `action/map/modify` | POST | `PUT /api/maps/{id}` |
| `action/map/get` | POST | `GET /api/maps/{id}` |
| `action/map/listByUser` | POST | `GET /api/maps?userId={id}` |
| `action/map/getLegend` | GET | `GET /api/maps/legend?name={sld}` |
| `action/style/create` | POST | `POST /api/styles` |
| `action/style/list` | POST | `GET /api/styles` |
| `action/user/create` | POST | `POST /api/users` |
| `admin/` (home) | GET | `GET /api/admin` |
| `admin/data-files` | GET | `GET /api/admin/data-files` |
| `admin/load-data-file` | GET | `POST /api/admin/load-data-file` |
| `layerServer/*` (proxy) | ANY | Nginx reverse proxy or `GET /api/layer-proxy/**` |

---

## Target Modern Stack

### Backend (`seismap-backend/`)
- **Java 21** (LTS) + **Spring Boot 3.4**
- **Spring Data JPA** + **Hibernate 6** + `hibernate-spatial` (replaces manual Hibernate Session + XML config)
- **LocationTech JTS** (replaces `com.vividsolutions.jts`, same API, new package)
- **Jackson FasterXML** (auto-configured by Spring Boot, replaces Codehaus Jackson 1.x)
- **PostgreSQL 16** + **PostGIS 3.x** via Docker
- **Spring Security** with JWT for authentication (replaces hardcoded credentials)
- **OpenAPI / Swagger** (`springdoc-openapi`) for API documentation and testing
- **Flyway** for database migrations
- **Maven** (keeping Maven for continuity)
- SRID 900913 (Google Mercator) conversions preserved via `CoordinatesConverter` equivalent

### Frontend (`seismap-frontend/`)
- **React 18+** with **TypeScript**, bundled with **Vite**
- **MUI (Material UI)** ✅ for UI components (DataGrid replaces ExtJS grids; Dialogs replace ExtJS windows)
- **OpenLayers 10** ([ol](file:///home/erecio/Documents/Projects/seismap/src/main/java/com/seismap/controller/MapController.java#35-108) npm package) for map rendering (handles thousands of points via WebGL)
- **React Router** for page navigation
- **Zustand** or **Redux Toolkit** for state management (replaces ExtJS Stores)
- **Axios** for HTTP client

### DevOps & Deployment
- **Docker Compose** orchestrating:
  - `postgis/postgis:16-3.4` (database)
  - `kartoza/geoserver` ✅ (renders depth points for tectonic plate visualization)
  - Backend JAR via `eclipse-temurin:21-jre`
  - Frontend static build via `nginx:alpine`
- **GeoServer proxying**: Nginx will handle `/layerServer/` proxying directly, removing the Java proxy hack.
- **Authentication**: Deferred to a future iteration. V1 will use a hardcoded user, same as legacy.
- **User/Client management**: Deferred. Not needed for V1.

---

## Confirmed Decisions

| Decision | Choice |
|:---|:---|
| UI Library | **MUI (Material UI)** |
| Mapping | **OpenLayers 10** (WebGL for thousands of points) |
| GeoServer | **Docker service** (`kartoza/geoserver`) — needed for depth/tectonic plate rendering |
| Authentication | **Deferred** — hardcoded user for V1 |
| User management | **Deferred** |
| Admin data-load | **Included** in V1 |

> [!NOTE]
> All decisions confirmed. Ready to proceed to execution phase.
