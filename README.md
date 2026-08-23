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
- **Consola de Administración:** [http://localhost:3000/admin](http://localhost:3000/admin) *(Requiere Modo Desarrollo)*
> El Backend se encarga de configurar automáticamente GeoServer (crear workspaces, datastores y publicar las capas WMS) al iniciar. Asegurate de que el backend haya terminado de iniciar completamente para que los mapas carguen correctamente.

### Modos de Ejecución (Producción vs Desarrollo)

Por defecto, la aplicación se levanta en modo **producción**. Esto significa que las herramientas de desarrollo y administración (Swagger UI, consola web de GeoServer, y la consola de administración `/admin`) están **bloqueadas y ocultas** por seguridad, tanto a nivel de servidor (nginx devuelve 404) como a nivel de cliente (el router de React no registra la ruta).

Para levantar el proyecto en modo **desarrollo** y habilitar el acceso a estas herramientas:

1. Copiá `.env.example` a `.env` en la raíz del proyecto (junto a `docker-compose.yml`) si todavía no lo hiciste, y editá la variable:
   ```env
   APP_PROFILE=development
   ```
   `.env` no se versiona (está en `.gitignore`) porque ahí también van las contraseñas — ver [Variables de entorno](#variables-de-entorno-docker-compose) y [Seguridad](#seguridad).
2. Ejecutá el comando de Docker Compose forzando la reconstrucción del frontend (para inyectar la variable):
   ```bash
   docker compose up -d --build
   ```

Una vez levantado, además de tener el botón de `Administración` en el mapa, vas a poder acceder tranquilamente a Swagger, GeoServer y la consola de administración `/admin` mediante las URLs detalladas arriba.
Para volver a producción, cambiá la variable a `production` (o borrala) y volvé a correr `docker compose up -d --build`.

### Apagar el proyecto

Para detener la ejecución de los contenedores y liberar los puertos, ejecutá el siguiente comando dentro del directorio del proyecto (`seismap_2026`):

```bash
# Apagar los contenedores conservando los datos de la base de datos (PostgreSQL) y GeoServer
docker compose down
```

> [!TIP]
> Si en algún momento necesitás limpiar todos los volúmenes de datos y arrancar una base de datos 100% en blanco desde cero, podés usar `docker compose down -v`. **Atención:** esto borrará todos los sismos y configuraciones guardadas en la BD local.

## Ejecutar como servicio de Linux (systemd)

Si querés que Seismap arranque como un servicio del sistema al boot, el proyecto ya incluye una unidad `systemd` basada en Docker Compose.

### 1. Ubicar el proyecto en una ruta estable

La unidad incluida apunta por defecto a:

```bash
/opt/seismap
```

Si vas a usar otra ruta, el instalador permite cambiarla con `TARGET_DIR`.

### 2. Instalar la unidad

Desde la raíz del proyecto:

```bash
sudo TARGET_DIR=/home/erecio/Documents/Projects/seismap_2026 ./scripts/install-systemd-service.sh
```

Si antes copiaste el proyecto a `/opt/seismap`, alcanza con:

```bash
sudo ./scripts/install-systemd-service.sh
```

El script:

- copia la unidad a `/etc/systemd/system/seismap.service`
- ajusta `WorkingDirectory` a la ruta indicada
- ejecuta `systemctl daemon-reload`
- deja el servicio habilitado para iniciar con el sistema

### 3. Operarlo

```bash
sudo systemctl start seismap
sudo systemctl restart seismap
sudo systemctl status seismap
sudo journalctl -u seismap -f
```

La unidad ejecuta:

```bash
docker compose up -d --build
```

al iniciar o recargar el servicio, y:

```bash
docker compose stop
```

al detenerlo.

> [!NOTE]
> Este servicio depende de que Docker esté instalado y levantado. Si cambiás el código o el `docker-compose.yml`, podés aplicar los cambios con `sudo systemctl restart seismap`.

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

El puerto de Postgres (`5432`) está publicado solo en loopback (`127.0.0.1`), así que podés conectarte con cualquier cliente externo (DBeaver, pgAdmin, DataGrip, etc.) **desde la misma máquina** donde corre Docker:

- **Host**: `localhost` o `127.0.0.1`
- **Puerto**: `5432`
- **Base de Datos**: `seismap`
- **Usuario**: el valor de `POSTGRES_USER` en tu `.env` (`seismap` por defecto)
- **Contraseña**: el valor de `POSTGRES_PASSWORD` en tu `.env`

Si el proyecto corre en un servidor remoto (por ejemplo la VM de producción), conectate a través de un túnel SSH en vez de exponer el puerto a internet:
```bash
ssh -L 5432:localhost:5432 usuario@tu-servidor
```
y despues apuntá tu cliente a `localhost:5432` como si fuera local.

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

Definilas en tu `.env` (copiá `.env.example` como punto de partida).

| Variable | Default | Descripción |
|---|---|---|
| `APP_PROFILE` | `production` | Perfil de ejecución. `production` oculta Swagger, GeoServer y la página de admin. `development` los habilita. Se aplica al backend (Spring profile) y al frontend (build arg + nginx). |
| `POSTGRES_USER` | `seismap` | Usuario de PostgreSQL. Usado por el contenedor `postgres` y por el backend (`SPRING_DATASOURCE_USERNAME`). |
| `POSTGRES_PASSWORD` | `seismap` | Contraseña de PostgreSQL. **Cambiala antes de deployar en cualquier servidor accesible desde afuera.** |
| `GEOSERVER_ADMIN_USER` | `admin` | Usuario admin de GeoServer. Usado por el contenedor `geoserver` y por el backend para autenticarse contra la REST API de GeoServer. |
| `GEOSERVER_ADMIN_PASSWORD` | `geoserver` | Contraseña admin de GeoServer. **Cambiala también** — `admin`/`geoserver` es la credencial default de GeoServer, ampliamente conocida. |
| `SEISMAP_GEOSERVER_URL` | `http://geoserver:8080/geoserver` | URL interna de GeoServer |
| `SEISMAP_DATA_FILES_DIRECTORY` | `/app/data` | Directorio de archivos `.data` para carga admin |

## Archivos de datos sísmicos

Colocá los archivos `.data` en la carpeta `data/` en la raíz del proyecto.
Luego accedé a `http://localhost:3000/admin` para cargarlos desde la UI.

## Seguridad

Antes de levantar esto en un servidor accesible desde internet (no solo tu red local):

1. **Cambiá las contraseñas por defecto.** Copiá `.env.example` a `.env` y definí `POSTGRES_PASSWORD` y `GEOSERVER_ADMIN_PASSWORD` con valores propios — los defaults (`seismap`, `geoserver`) son públicos (están en este mismo repo) y `admin`/`geoserver` además es la credencial default conocida de GeoServer.
2. **Usá siempre `APP_PROFILE=production`** en el servidor — oculta Swagger, la UI de GeoServer y la consola `/admin`, tanto en nginx como a nivel Spring (`springdoc.swagger-ui.enabled=false`).
3. **No publiques el puerto de Postgres a internet.** `docker-compose.yml` ya lo ata a `127.0.0.1` — no lo cambies a `"5432:5432"` en un host público. Para administrar la BD de forma remota, usá un túnel SSH (ver [Conectar a la Base de Datos](#conectar-a-la-base-de-datos-postgis)).
4. **La REST API de GeoServer y los endpoints `/api/admin/*` del backend están bloqueados por nginx en modo producción** (devuelven 404), porque no tienen autenticación propia. Si necesitás disparar una carga de datos o la importación de USGS en el servidor, hacelo desde adentro del contenedor en vez de por la URL pública:
   ```bash
   docker exec seismap-backend wget -qO- --post-data='' http://localhost:8080/api/admin/import-usgs
   ```
5. Este proyecto **no tiene autenticación de usuarios** (queda para una iteración futura — ver `docs/00-original-migration-plan.md`). Todo lo que quede accesible públicamente en modo `production` (el mapa, las capas WMS/WFS) es de solo lectura desde la perspectiva de un visitante externo; los puntos 3 y 4 cubren las superficies de escritura/administración.
6. **Servir la app por HTTPS en cualquier host accesible desde internet** — ver la sección [HTTPS / TLS](#https--tls) debajo. Sin esto, todo el tráfico (incluidas eventuales cargas de datos desde `/admin`) viaja sin cifrar.

## HTTPS / TLS

El contenedor `frontend` expone tanto HTTP (puerto interno `80`) como HTTPS (puerto interno `443`) en el mismo nginx — ver `seismap-frontend/nginx.prod.conf` / `nginx.dev.conf`. En `docker-compose.yml` estos se publican como `3000:80` y `3443:443`.

### Cómo se emite el certificado

El certificado se pide y renueva con **Certbot corriendo directamente en el host** (no en un contenedor) usando **validación DNS-01 contra la API de DuckDNS**, así que no hace falta abrir el puerto 80 al público:

```bash
sudo apt-get install -y certbot

sudo certbot certonly \
  --manual \
  --preferred-challenges dns \
  --manual-auth-hook /ruta/a/certbot-auth-hook.sh \
  --manual-cleanup-hook /ruta/a/certbot-cleanup-hook.sh \
  -d TU-SUBDOMINIO.duckdns.org \
  --deploy-hook "docker exec seismap-frontend nginx -s reload" \
  --agree-tos -m tu-email@ejemplo.com --non-interactive
```

Los scripts `certbot-auth-hook.sh` / `certbot-cleanup-hook.sh` publican y limpian el TXT `_acme-challenge` llamando a `https://www.duckdns.org/update?...&txt=...` con el mismo token que ya usás para mantener la IP actualizada — no están versionados en este repo porque contienen ese token; viven junto al script de actualización de DuckDNS en el host (p. ej. `~/duckdns/`).

Certbot instala su propio timer de renovación automática (`certbot.timer`, vía el paquete de `apt`); el `--deploy-hook` queda guardado en `/etc/letsencrypt/renewal/*.conf` y recarga nginx solo después de cada renovación exitosa.

### Cómo lo consume el contenedor

`docker-compose.yml` monta `/etc/letsencrypt:/etc/letsencrypt:ro` (de solo lectura) en el contenedor `frontend`. Al arrancar, `docker-entrypoint.sh`:

- si encuentra `/etc/letsencrypt/live/<dominio>/{fullchain,privkey}.pem`, los symlinkea a las rutas fijas que usa nginx (`/etc/nginx/ssl/*.pem`);
- si no (por ejemplo en una máquina de desarrollo local, sin certificado real), genera un certificado autofirmado de una sola vez, para que nginx igual pueda arrancar y servir HTTPS localmente (con warning de seguridad esperable en el navegador).

Si cambiás el dominio, actualizá el nombre `erecio.duckdns.org` hardcodeado en `nginx.prod.conf`, `nginx.dev.conf` y `docker-entrypoint.sh`.

### Puertos en el router

Como el puerto 80/443 estándar no está expuesto en este deployment, el acceso público es por un puerto no estándar reenviado en el router de casa hacia la IP interna de la VM, por ejemplo `TCP 3443 → <ip-interna>:3443`. Ajustá el número de puerto (y el mapeo en `docker-compose.yml`) según lo que hayas configurado ahí.

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
├── data/                   Archivos .data
├── docker-compose.yml      Stack completo
└── docs/02-task-checklist.md  Checklist de migración
```
