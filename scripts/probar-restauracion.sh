#!/usr/bin/env sh
# Simulacro de restauracion (mensual): restaura el ultimo respaldo de
# "oncologia" en una base TEMPORAL y compara el conteo de las tablas que
# sostienen los indicadores con la base en uso. No toca la base real.
#
#   scripts/probar-restauracion.sh
#
# Un respaldo que nunca se probo restaurar no es un respaldo.
set -eu

# El compose y la carpeta respaldos/ viven en docker/ (un nivel arriba de este script).
cd "$(dirname "$0")/../docker"
COMPOSE="docker compose -f docker-compose.yml -f docker-compose.prod.yml"
ARCHIVO="${1:-respaldos/last/oncologia-latest.sql.gz}"
TEMPORAL="oncologia_simulacro"
TABLAS="paciente cita participante_estudio estudio_fase medicion_registro consulta recordatorio auditoria_accion"

[ -f "$ARCHIVO" ] || { echo "No existe $ARCHIVO (el servicio 'respaldo' aun no genero uno)" >&2; exit 1; }

psql_en() { $COMPOSE exec -T postgres psql -v ON_ERROR_STOP=1 -q -At -U oncologia -d "$1" -c "$2"; }

psql_en postgres "DROP DATABASE IF EXISTS $TEMPORAL"
psql_en postgres "CREATE DATABASE $TEMPORAL"
trap 'psql_en postgres "DROP DATABASE IF EXISTS $TEMPORAL" >/dev/null' EXIT

gunzip -c "$ARCHIVO" | $COMPOSE exec -T postgres psql -v ON_ERROR_STOP=1 -q -U oncologia -d "$TEMPORAL" >/dev/null

echo "Respaldo: $ARCHIVO ($(date -r "$ARCHIVO" '+%Y-%m-%d %H:%M' 2>/dev/null || echo 'fecha desconocida'))"
printf "%-22s %12s %12s\n" "tabla" "respaldo" "en uso"
for tabla in $TABLAS; do
  printf "%-22s %12s %12s\n" "$tabla" \
    "$(psql_en "$TEMPORAL" "SELECT COUNT(*) FROM $tabla")" \
    "$(psql_en oncologia "SELECT COUNT(*) FROM $tabla")"
done
echo "Migraciones en el respaldo: $(psql_en "$TEMPORAL" "SELECT MAX(version) FROM flyway_schema_history WHERE success")"
echo "Simulacro OK: el respaldo se restaura completo (en uso puede tener filas posteriores al respaldo)."
