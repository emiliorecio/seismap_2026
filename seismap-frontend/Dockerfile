# ─── Stage 1: Build React app ────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /build
COPY package*.json ./
RUN npm ci --silent

COPY . .
RUN npm run build

# ─── Stage 2: Serve with Nginx ───────────────────────────────────────────────
FROM nginx:1.27-alpine

# Remove default nginx site
RUN rm -f /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/seismap.conf
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
