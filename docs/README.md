# Seismap — Documentación de Desarrollo

Índice de la documentación técnica del proyecto Seismap.

## Archivos

| Archivo | Descripción |
|:---|:---|
| [00-original-migration-plan.md](./00-original-migration-plan.md) | Plan original de migración: análisis del legado, decisiones de stack, mapping de APIs |
| [01-migration-plan.md](./01-migration-plan.md) | Arquitectura Docker y nginx del proyecto migrado |
| [02-task-checklist.md](./02-task-checklist.md) | Checklist detallado de progreso por fase |
| [03-geoserver-integration.md](./03-geoserver-integration.md) | Implementación de integración WMS con GeoServer |
| [04-pending-features.md](./04-pending-features.md) | Funcionalidades pendientes: polígono de selección, detalle de evento |

## Stack Tecnológico

| Capa | Tecnología |
|:---|:---|
| Backend | Java 21 + Spring Boot 3.4 + Hibernate Spatial + Flyway |
| Frontend | React 18 + TypeScript + OpenLayers + MUI |
| Base de datos | PostgreSQL 16 + PostGIS 3.4 |
| GeoServer | kartoza/geoserver:2.26.1 |
| Contenedores | Docker Compose |

## Logs

### Nginx (tráfico HTTP)

El access log de Nginx se encuentra **dentro del contenedor** `seismap-frontend`, en `/var/log/nginx/access.log`.

> **Importante:** el log solo se activa cuando `APP_PROFILE=production`. En modo `development` no se escribe nada.

```bash
# Ver logs en tiempo real
docker exec seismap-frontend tail -f /var/log/nginx/access.log

# O via docker logs
docker logs -f seismap-frontend
```

> Los logs no se persisten en disco del host (no hay volumen montado), por lo que se pierden si el contenedor se reinicia.

### Backend (Spring Boot)

Los logs del backend se emiten por stdout y se pueden ver con:

```bash
docker logs -f seismap-backend
```

El nivel de log está configurado en `application.yml`:
- `com.seismap` → `DEBUG`
- `org.hibernate.SQL` → `DEBUG`

## Entorno

```bash
# Levantar todo el stack
docker compose up --build -d

# Frontend: http://localhost:3000
# Backend API: http://localhost:8080/api
# GeoServer: http://localhost:8600/geoserver
# Swagger UI: http://localhost:8080/swagger-ui.html
```
