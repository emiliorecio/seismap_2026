# TODO

## Seguridad (endurecimiento del deployment en internet)

- [ ] **CORS abierto** (`allowedOriginPatterns: ["*"]`) — riesgo bajo hoy porque `allowCredentials: false` (no hay cookies/sesión que robar), pero cualquier sitio puede pegarle a la API desde el navegador de un visitante. Acotar a los orígenes reales de la app.
- [ ] **Sin rate limiting** en la API ni en el WMS/WFS de GeoServer — nada impide abuso de recursos vía muchísimas requests.
- [ ] **Revisar el firewall a nivel VM** (`ufw`/iptables), más allá de los puertos que publica Docker. Durante el diagnóstico de TLS se vieron abiertos, entre otros: RDP (3389), VNC (5900, solo loopback), WireGuard (`wg-easy`, 51820/udp + 51821/tcp), y un par de listeners sin identificar en 3005 y 3030 — vale la pena confirmar qué son y si deberían estar expuestos.

## Bugs (no seguridad)

- [ ] `GET /api/application` responde 500 (`LazyInitializationException`) — encontrado de casualidad, todavía sin investigar.
