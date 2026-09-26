#!/bin/bash
set -e

# Setup OpenJDK & Maven PATH on macOS if present
if [ -d "/opt/homebrew/opt/openjdk/bin" ]; then
    export PATH="/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$PATH"
fi

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

# Load environment variables if .env exists
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
fi

# Ensure AI_API_KEY is available (read from environment, with optional .env fallback)
if [ -z "$AI_API_KEY" ] && [ -f ".env" ]; then
    ENV_VAL=$(grep -E '^AI_API_KEY=' .env 2>/dev/null | cut -d '=' -f2- | tr -d '"' | tr -d "'" || true)
    if [ -n "$ENV_VAL" ]; then
        export AI_API_KEY="$ENV_VAL"
    fi
fi

JAR_FILE="backend/target/codingagent-backend-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "⚙️  Building backend artifact with Maven..."
    (cd backend && mvn package -DskipTests)
fi

export EVAL_MODE="true"
exec java -jar "$JAR_FILE" --eval "$@"
