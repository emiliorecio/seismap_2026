# Seismap

Aplicación web de visualización de eventos sísmicos migrada a stack moderno.

## Stack

| Componente | Tecnología |
|---|---|
| Frontend | React 18 + TypeScript + Vite + MUI + OpenLayers 10 |
| Backend | Spring Boot 3.4.3 + Java 21 + Hibernate Spatial |
| Base de datos | PostgreSQL 16 + PostGIS 3.4 |
| Servidor de mapas | GeoServer 2.26.1 |
| Reverse proxy | Nginx 1.27 |

## Requisitos

- Docker Engine 24+
- Docker Compose v2.5+
- (Desarrollo local) Java 21, Maven 3.9+, Node 20+

## Inicio rápido (Docker)

Para levantar el proyecto completo (Frontend, Backend, Base de Datos y GeoServer) utilizando Docker:

```bash
# 1. Navegar al directorio del proyecto nuevo
cd /home/erecio/Documents/Projects/seismap_2026

# 2. Construir y levantar toda la infraestructura en segundo plano
docker compose up -d --build
```

Una vez que los contenedores estén corriendo (puede demorar un poco la primera vez construyendo las imágenes de Java y Node), podés acceder a los diferentes servicios en las siguientes URLs:

- **Aplicación Web (Frontend):** [http://localhost:3000](http://localhost:3000)
- **Backend (API Swagger):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **GeoServer:** [http://localhost:8600/geoserver](http://localhost:8600/geoserver) *(Credenciales: `admin` / `geoserver`)*

> [!NOTE]
> El Backend se encarga de configurar automáticamente GeoServer (crear workspaces, datastores y publicar las capas WMS) al iniciar. Asegurate de que el backend haya terminado de iniciar completamente para que los mapas carguen correctamente.

## Desarrollo local

### Backend

```bash
cd seismap-backend

# Requiere Java 21 (usa SDKMAN)
sdk use java 21.0.2-open

# Asegurate que Postgres esté corriendo (con Docker):
docker compose up -d postgres

# Correr backend
JAVA_HOME=~/.sdkman/candidates/java/21.0.2-open mvn spring-boot:run
```

El backend levanta en `http://localhost:8080`. Flyway crea las tablas automáticamente al iniciar.

### Frontend

```bash
cd seismap-frontend

npm install
npm run dev
# → http://localhost:5173
```

El proxy de Vite redirige `/api/*` y `/layerServer/*` al backend en `:8080`.

## Variables de entorno (docker-compose)

| Variable | Default | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/seismap` | URL de conexión a PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `seismap` | Usuario de BD |
| `SPRING_DATASOURCE_PASSWORD` | `seismap` | Password de BD |
| `SEISMAP_GEOSERVER_URL` | `http://geoserver:8080/geoserver` | URL interna de GeoServer |
| `SEISMAP_DATA_FILES_DIRECTORY` | `/app/data` | Directorio de archivos `.data` para carga admin |

## Archivos de datos sísmicos

Colocá los archivos `.data` en la carpeta `data/` en la raíz del proyecto.
Luego accedé a `http://localhost:3000/admin` para cargarlos desde la UI.

## Estructura del proyecto

```
seismap/
├── seismap-backend/        Spring Boot 3.4 (Java 21)
│   ├── src/main/java/com/seismap/
│   │   ├── controller/     REST controllers
│   │   ├── service/        Business logic
│   │   ├── model/entity/   JPA entities
│   │   ├── model/enums/    Domain enums
│   │   └── repository/     Spring Data repositories
│   └── src/main/resources/db/migration/  Flyway migrations
│
├── seismap-frontend/       React + Vite + TypeScript
│   ├── src/
│   │   ├── components/     SeismapMapView, MainLayout, panels
│   │   ├── pages/          AdminPage
│   │   ├── services/       API calls (Axios)
│   │   ├── store/          Zustand global state
│   │   └── types/          TypeScript types
│   └── nginx.conf          Nginx config (producción)
│
├── data/                   Archivos .data (no versionados)
├── docker-compose.yml      Stack completo
└── task.md                 Checklist de migración
```
