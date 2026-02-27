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

# Set default env var so envsubst doesn't fail
ENV APP_PROFILE=production

# Remove default nginx site
RUN rm -f /etc/nginx/conf.d/default.conf

# Copy template and use entrypoint substitution
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
