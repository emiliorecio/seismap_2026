# Plan de Migración — Seismap

Modernización de la aplicación Seismap 2011 (Java 1.5 / Spring 3.2 / ExtJS / OpenLayers 2) al stack moderno.

## Stack Legacy → Nuevo

| Componente | Legacy | Nuevo |
|:---|:---|:---|
| Java | 1.5 | 21 |
| Framework | Spring 3.2 (XML) | Spring Boot 3.4 |
| ORM | Hibernate 4 + Hibernate Spatial | Spring Data JPA + Hibernate Spatial |
| Base de datos | PostgreSQL 9.1 + PostGIS 1.3 | PostgreSQL 16 + PostGIS 3.4 |
| Frontend | ExtJS 3 + GeoExt | React 18 + OpenLayers 8 + MUI |
| Build | Ant/Maven WAR | Maven JAR (Spring Boot embedded) |
| Deploy | Tomcat standalone | Docker Compose (4 servicios) |
| GeoServer | Externo | kartoza/geoserver:2.26.1 en Docker |

## Arquitectura de Servicios Docker

```
┌──────────────────────────────────────────────────────┐
│                   Docker Compose                     │
│                                                      │
│  ┌──────────┐    ┌──────────┐    ┌───────────────┐  │
│  │ postgres │    │geoserver │    │   backend     │  │
│  │  :5432   │◄───│  :8080   │◄───│   :8080       │  │
│  │ PostGIS  │    │WMS/WFS   │    │ Spring Boot   │  │
│  └────┬─────┘    └──────────┘    └───────┬───────┘  │
│       │                                  │           │
│       └──────────────┬───────────────────┘           │
│                      ▼                               │
│               ┌──────────────┐                       │
│               │  frontend    │                       │
│               │  :3000       │                       │
│               │  React+Nginx │                       │
│               └──────────────┘                       │
└──────────────────────────────────────────────────────┘
```

## Nginx Proxy Rules

| Path | Destino |
|:---|:---|
| `/` | SPA React (index.html) |
| `/api/*` | `backend:8080` |
| `/geoserver/*` | `geoserver:8080/geoserver/` |

## GeoServer Auto-Configuración

Al arrancar, `GeoServerAutoConfigService` configura automáticamente via REST API:
1. Workspace: `seismap`
2. Datastore: `seismap_postgis` (conexión a PostgreSQL)
3. Layer: `eventandaveragemagnitudes` (vista materializada)
4. Estilos SLD: 7 estilos temáticos (ver `03-geoserver-integration.md`)
