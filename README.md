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
- **Backend (API Swagger):** [http://localhost:3000/swagger-ui/](http://localhost:3000/swagger-ui/) *(Requiere Modo Desarrollo)*
- **GeoServer Web UI:** [http://localhost:3000/geoserver/web/](http://localhost:3000/geoserver/web/) *(Credenciales: `admin` / `geoserver` - Requiere Modo Desarrollo)*
> El Backend se encarga de configurar automáticamente GeoServer (crear workspaces, datastores y publicar las capas WMS) al iniciar. Asegurate de que el backend haya terminado de iniciar completamente para que los mapas carguen correctamente.

### Modos de Ejecución (Producción vs Desarrollo)

Por defecto, la aplicación se levanta en modo **producción**. Esto significa que las herramientas de desarrollo y administración (Swagger UI, consola web de GeoServer y página de carga de datos) están **bloqueadas y ocultas** por seguridad, forzando a que todo el tráfico pase puramente por el frontend en el puerto 3000.

Para levantar el proyecto en modo **desarrollo** y habilitar el acceso a estas herramientas:

1. Creá o editá el archivo `.env` en la raíz del proyecto (junto a `docker-compose.yml`) y agregá la siguiente variable:
   ```env
   APP_PROFILE=development
   ```
2. Ejecutá el comando de Docker Compose forzando la reconstrucción del frontend (para inyectar la variable):
   ```bash
   docker compose up -d --build
   ```

Una vez levantado, además de tener el botón de `Administración` en el mapa, vas a poder acceder tranquilamente a Swagger y GeoServer mediante las URLs detalladas arriba.
Para volver a producción, cambiá la variable a `production` (o borrala) y volvé a correr `docker compose up -d --build`.

### Apagar el proyecto

Para detener la ejecución de los contenedores y liberar los puertos, ejecutá el siguiente comando dentro del directorio del proyecto (`seismap_2026`):

```bash
# Apagar los contenedores conservando los datos de la base de datos (PostgreSQL) y GeoServer
docker compose down
```

> [!TIP]
> Si en algún momento necesitás limpiar todos los volúmenes de datos y arrancar una base de datos 100% en blanco desde cero, podés usar `docker compose down -v`. **Atención:** esto borrará todos los sismos y configuraciones guardadas en la BD local.

### Ver logs (Consola)

Para monitorear o diagnosticar algún problema, podés ver el flujo en tiempo real (logs) del Backend o cualquier otro componente utilizando Docker:

```bash
# Ver los logs del backend y seguirlos en vivo
docker compose logs -f backend

# Ver los logs del frontend y seguirlos en vivo
docker compose logs -f frontend

# Para ver todos los servicios al mismo tiempo
docker compose logs -f

# Para ver todos los conexion en produccions
tail -f /var/log/nginx/access.log
```

### Conectar a la Base de Datos (PostGIS)

Podés conectarte a la base de datos PostgreSQL/PostGIS del contenedor desde cualquier cliente externo (como DBeaver, pgAdmin, DataGrip, etc.) utilizando las siguientes credenciales expuestas en tu red local (puerto `5432`):

- **Host**: `localhost` o `127.0.0.1`
- **Puerto**: `5432`
- **Base de Datos**: `seismap`
- **Usuario**: `seismap`
- **Contraseña**: `seismap`

> [!NOTE]
> La base de datos incluye la extensión `postgis` habilitada y expone las vistas espacializadas o vistas materializadas de `eventandaveragemagnitudes` utilizadas por GeoServer.

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
| `APP_PROFILE` | `production` | Perfil de ejecución. `production` oculta Swagger, GeoServer y la página de admin. `development` los habilita. Se aplica al backend (Spring profile) y al frontend (build arg + nginx). |
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
│   └── nginx.conf.template Nginx config proxy (producción)
│
├── data/                   Archivos .data (no versionados)
├── docker-compose.yml      Stack completo
└── task.md                 Checklist de migración
```
