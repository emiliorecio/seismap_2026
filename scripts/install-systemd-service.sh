#!/usr/bin/env bash

set -euo pipefail

SERVICE_NAME="${SERVICE_NAME:-seismap}"
TARGET_DIR="${TARGET_DIR:-/opt/seismap}"
SYSTEMD_DIR="${SYSTEMD_DIR:-/etc/systemd/system}"
SERVICE_PATH="${SYSTEMD_DIR}/${SERVICE_NAME}.service"
TEMPLATE_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/deploy/systemd/seismap.service"

if [[ ! -f "${TEMPLATE_PATH}" ]]; then
  echo "No se encontro la plantilla del servicio en ${TEMPLATE_PATH}" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker no esta instalado o no esta en PATH" >&2
  exit 1
fi

if [[ ! -d "${TARGET_DIR}" ]]; then
  echo "No existe ${TARGET_DIR}. Copia este proyecto ahi o exporta TARGET_DIR=/ruta/al/proyecto" >&2
  exit 1
fi

mkdir -p "${SYSTEMD_DIR}"
sed "s|^WorkingDirectory=.*$|WorkingDirectory=${TARGET_DIR}|" "${TEMPLATE_PATH}" > "${SERVICE_PATH}"

systemctl daemon-reload
systemctl enable "${SERVICE_NAME}.service"

cat <<EOF
Servicio instalado en: ${SERVICE_PATH}

Comandos utiles:
  sudo systemctl start ${SERVICE_NAME}
  sudo systemctl status ${SERVICE_NAME}
  sudo systemctl restart ${SERVICE_NAME}
  sudo journalctl -u ${SERVICE_NAME} -f
EOF
