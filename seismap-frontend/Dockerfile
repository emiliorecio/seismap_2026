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

# Select static nginx config based on build profile (no envsubst needed).
# development → nginx.dev.conf  |  any other value → nginx.prod.conf
ARG APP_PROFILE=production
COPY nginx.dev.conf nginx.prod.conf /tmp/nginx-profiles/
RUN if [ "${APP_PROFILE}" = "development" ]; then \
        cp /tmp/nginx-profiles/nginx.dev.conf /etc/nginx/conf.d/default.conf; \
    else \
        cp /tmp/nginx-profiles/nginx.prod.conf /etc/nginx/conf.d/default.conf; \
    fi && \
    rm -rf /tmp/nginx-profiles

COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
