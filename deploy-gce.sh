#!/usr/bin/env bash
set -euo pipefail

# ── Configuration ─────────────────────────────────────────
APP_NAME="finance-dashboard"
APP_PORT="8085"                          # external port (8080 is taken by supabase-meta)
DB_HOST="supabase-db"                    # container name on the Docker network
DB_PORT="5432"
DB_NAME="finance_db"
DB_USER="postgres"
DB_PASS="${SUPABASE_DB_PASSWORD:-your-super-secret-and-long-postgres-password}"
JWT_SECRET="${JWT_SECRET:-$(openssl rand -hex 32)}"

# Detect the Supabase Docker network
SUPA_NETWORK=$(docker inspect supabase-db --format='{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}' 2>/dev/null || echo "")
if [ -z "$SUPA_NETWORK" ]; then
  echo "ERROR: supabase-db container not found. Is Supabase running?"
  exit 1
fi
echo "Found Supabase network: $SUPA_NETWORK"

# ── Step 1: Create finance_db in Supabase Postgres (if not exists) ──
echo "Ensuring database '$DB_NAME' exists..."
docker exec supabase-db psql -U "$DB_USER" -tc \
  "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" \
  | grep -q 1 \
  || docker exec supabase-db psql -U "$DB_USER" -c "CREATE DATABASE $DB_NAME"
echo "Database ready."

# ── Step 2: Build the Docker image ──────────────────────
echo "Building Docker image..."
docker build -t "$APP_NAME" .

# ── Step 3: Stop old container if running ────────────────
if docker ps -a --format '{{.Names}}' | grep -q "^${APP_NAME}$"; then
  echo "Removing old container..."
  docker rm -f "$APP_NAME"
fi

# ── Step 4: Run the container ────────────────────────────
echo "Starting $APP_NAME on port $APP_PORT..."
docker run -d \
  --name "$APP_NAME" \
  --network "$SUPA_NETWORK" \
  --restart unless-stopped \
  -p "${APP_PORT}:8080" \
  -e SPRING_PROFILES_ACTIVE=gce \
  -e DB_HOST="$DB_HOST" \
  -e DB_PORT="$DB_PORT" \
  -e DB_NAME="$DB_NAME" \
  -e DB_USERNAME="$DB_USER" \
  -e DB_PASSWORD="$DB_PASS" \
  -e JWT_SECRET="$JWT_SECRET" \
  "$APP_NAME"

echo ""
echo "═══════════════════════════════════════════════════════"
echo "  $APP_NAME is running!"
echo "  Local:    http://localhost:$APP_PORT"
echo "  External: http://<YOUR_VM_IP>:$APP_PORT"
echo "  JWT_SECRET=$JWT_SECRET"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Useful commands:"
echo "  docker logs -f $APP_NAME     # view logs"
echo "  docker restart $APP_NAME     # restart"
echo "  docker rm -f $APP_NAME       # remove"
