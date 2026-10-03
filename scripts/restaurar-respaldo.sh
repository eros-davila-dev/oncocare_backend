#!/usr/bin/env sh
# Restaura un respaldo de docker/respaldos sobre una base del PostgreSQL de produccion.
#
#   scripts/restaurar-respaldo.sh docker/respaldos/last/oncologia-latest.sql.gz [base]
#
# Por defecto restaura sobre "oncologia". Detiene el backend y n8n mientras
# dura (nadie debe escribir a mitad de la restauracion) y los vuelve a
# levantar al final. Es destructivo: pide confirmacion escribiendo el nombre
# de la base.
set -eu

ARCHIVO="${1:?Uso: $0 <respaldo.sql.gz> [base]}"
BASE="${2:-oncologia}"
[ -f "$ARCHIVO" ] || { echo "No existe $ARCHIVO" >&2; exit 1; }
ARCHIVO="$(cd "$(dirname "$ARCHIVO")" && pwd)/$(basename "$ARCHIVO")"
# El compose vive en docker/ (un nivel arriba de este script).
cd "$(dirname "$0")/../docker"
COMPOSE="docker compose -f docker-compose.yml -f docker-compose.prod.yml"

echo "Se reemplazara el contenido de la base '$BASE' con $ARCHIVO."
printf "Escriba el nombre de la base para confirmar: "
read -r CONFIRMACION
[ "$CONFIRMACION" = "$BASE" ] || { echo "Cancelado."; exit 1; }

$COMPOSE stop backend n8n
trap '$COMPOSE start backend n8n' EXIT

# Los respaldos se generan con --clean --if-exists: borran y recrean cada
# objeto, asi que se restauran sobre la base existente.
gunzip -c "$ARCHIVO" | $COMPOSE exec -T postgres psql -v ON_ERROR_STOP=1 -q -U oncologia -d "$BASE"

echo "Restauracion completa. Verifique en la intranet: Estudio > Resultados y Auditoria."
