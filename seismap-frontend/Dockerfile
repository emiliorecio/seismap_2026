# ─── Stage 1: Build React app ────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /build
COPY package*.json ./
RUN npm ci --silent

COPY . .
ARG APP_PROFILE=production
RUN npm run build -- --mode ${APP_PROFILE}

# ─── Stage 2: Serve with Nginx ───────────────────────────────────────────────
FROM nginx:1.27-alpine

# Remove default nginx site
RUN rm -f /etc/nginx/conf.d/default.conf

# openssl CLI is needed by docker-entrypoint.sh to generate a self-signed
# fallback certificate when no real one is mounted (see there for why).
RUN apk add --no-cache openssl

# Select static nginx config based on build profile (no envsubst needed).
# development → nginx.dev.conf  |  any other value → nginx.prod.conf
ARG APP_PROFILE=production
COPY nginx.dev.conf nginx.prod.conf nginx-locations-dev.conf nginx-locations-prod.conf /tmp/nginx-profiles/
RUN if [ "${APP_PROFILE}" = "development" ]; then \
        cp /tmp/nginx-profiles/nginx.dev.conf /etc/nginx/conf.d/default.conf; \
        cp /tmp/nginx-profiles/nginx-locations-dev.conf /etc/nginx/locations.conf; \
    else \
        cp /tmp/nginx-profiles/nginx.prod.conf /etc/nginx/conf.d/default.conf; \
        cp /tmp/nginx-profiles/nginx-locations-prod.conf /etc/nginx/locations.conf; \
    fi && \
    rm -rf /tmp/nginx-profiles

COPY --from=builder /build/dist /usr/share/nginx/html

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80 443

CMD ["/docker-entrypoint.sh"]
