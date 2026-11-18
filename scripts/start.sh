#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"
if [[ ! -f .env ]]; then
  printf '%s\n' 'Maak eerst .env: kopieer .env.example en vul de database- en beheerdersgegevens in.' >&2
  exit 1
fi
set -a
# .env is a local shell-compatible configuration file; quote values containing special characters.
source .env
set +a
: "${POSTGRES_DB:?Vul POSTGRES_DB in .env in}"
: "${POSTGRES_USER:?Vul POSTGRES_USER in .env in}"
: "${POSTGRES_PASSWORD:?Vul POSTGRES_PASSWORD in .env in}"
if [[ "$POSTGRES_PASSWORD" == 'change-me' ]]; then printf '%s\n' 'Kies eerst een eigen POSTGRES_PASSWORD in .env.' >&2; exit 1; fi
for cmd in java node npm docker; do command -v "$cmd" >/dev/null || { printf 'Ontbrekend programma: %s\n' "$cmd" >&2; exit 1; }; done
if ! docker info >/dev/null 2>&1; then printf '%s\n' 'Docker is niet bereikbaar. Start Docker Desktop en probeer opnieuw.' >&2; exit 1; fi
if [[ -n "${BOOTSTRAP_ADMIN_EMAIL:-}" && -z "${BOOTSTRAP_ADMIN_PASSWORD:-}" ]] || [[ -z "${BOOTSTRAP_ADMIN_EMAIL:-}" && -n "${BOOTSTRAP_ADMIN_PASSWORD:-}" ]]; then printf '%s\n' 'Vul zowel BOOTSTRAP_ADMIN_EMAIL als BOOTSTRAP_ADMIN_PASSWORD in, of laat beide leeg.' >&2; exit 1; fi
printf '%s\n' 'PostgreSQL starten…'
docker compose up -d --wait db
if [[ -z "${BOOTSTRAP_ADMIN_EMAIL:-}" ]]; then
  roles_table="$(docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "SELECT to_regclass('public.user_roles') IS NOT NULL")"
  if [[ "$roles_table" != "t" ]]; then
    printf '%s\n' 'Vul voor de eerste start BOOTSTRAP_ADMIN_EMAIL en BOOTSTRAP_ADMIN_PASSWORD in .env in.' >&2; exit 1
  fi
  admin_count="$(docker compose exec -T db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc "SELECT count(*) FROM user_roles WHERE role = 'ADMIN'")"
  if [[ "$admin_count" == "0" ]]; then printf '%s\n' 'Er is nog geen beheerder. Vul BOOTSTRAP_ADMIN_EMAIL en BOOTSTRAP_ADMIN_PASSWORD in .env in.' >&2; exit 1; fi
fi
if [[ ! -d frontend/node_modules ]]; then (cd frontend && npm ci); fi
cleanup(){ trap - EXIT INT TERM; [[ -n "${backend_pid:-}" ]] && kill "$backend_pid" 2>/dev/null || true; [[ -n "${frontend_pid:-}" ]] && kill "$frontend_pid" 2>/dev/null || true; wait 2>/dev/null || true; }
trap cleanup EXIT INT TERM
printf '%s\n' 'Backend en frontend starten. Open http://localhost:5173 zodra beide gereed zijn. Stop met Ctrl+C.'
(cd backend && exec ./mvnw spring-boot:run) & backend_pid=$!
(cd frontend && exec npm run dev -- --host 127.0.0.1) & frontend_pid=$!
while kill -0 "$backend_pid" 2>/dev/null && kill -0 "$frontend_pid" 2>/dev/null; do sleep 2; done
printf '%s\n' 'Een van de applicatieprocessen is gestopt; controleer de melding hierboven.' >&2
exit 1
