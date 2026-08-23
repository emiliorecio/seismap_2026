#!/bin/sh
set -e

# nginx.{dev,prod}.conf point their ssl_certificate/ssl_certificate_key at
# these fixed paths. Populate them with the real Let's Encrypt cert when it's
# bind-mounted (production VM), or a self-signed fallback otherwise (local
# dev, CI, any machine without /etc/letsencrypt) so nginx always has
# something valid to start with.
CERT_DIR=/etc/nginx/ssl
LE_CERT=/etc/letsencrypt/live/erecio.duckdns.org/fullchain.pem
LE_KEY=/etc/letsencrypt/live/erecio.duckdns.org/privkey.pem

mkdir -p "$CERT_DIR"

if [ -f "$LE_CERT" ] && [ -f "$LE_KEY" ]; then
    ln -sf "$LE_CERT" "$CERT_DIR/fullchain.pem"
    ln -sf "$LE_KEY" "$CERT_DIR/privkey.pem"
elif [ ! -f "$CERT_DIR/fullchain.pem" ]; then
    echo "No Let's Encrypt certificate mounted — generating a self-signed fallback (local/dev only)."
    openssl req -x509 -nodes -newkey rsa:2048 -days 3650 \
        -keyout "$CERT_DIR/privkey.pem" \
        -out "$CERT_DIR/fullchain.pem" \
        -subj "/CN=localhost" 2>/dev/null
fi

exec nginx -g "daemon off;"
