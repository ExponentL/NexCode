#!/bin/bash
set -e

# Setup OpenJDK & Maven PATH on macOS if present
if [ -d "/opt/homebrew/opt/openjdk/bin" ]; then
    export PATH="/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$PATH"
fi

cd "$(dirname "$0")"

# Load environment variables if .env exists
if [ -f ".env" ]; then
    echo "Loading environment variables from .env..."
    export $(grep -v '^#' .env | xargs)
fi

echo "=================================================="
echo "🚀 Starting Nexcode Backend (Spring Boot)"
echo "   Java Version: $(java --version | head -n 1)"
echo "   Port: ${SERVER_PORT:-8080}"
echo "=================================================="

JAR_FILE="backend/target/codingagent-backend-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Building backend JAR with Maven..."
    (cd backend && mvn package -DskipTests)
fi

exec java -jar "$JAR_FILE"
