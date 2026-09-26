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

mkdir -p logs

echo "=========================================================="
echo "🤖 Autonomous AI Coding-Agent Harness"
echo "=========================================================="

# Check if ports 8080 or 5173 are currently occupied
PID_8080=$(lsof -ti :8080 2>/dev/null || true)
PID_5173=$(lsof -ti :5173 2>/dev/null || true)
if [ -n "$PID_8080" ] || [ -n "$PID_5173" ]; then
    echo "⚠️  Cleaning up previous processes on ports 8080 / 5173..."
    ./stop.sh
    sleep 1
fi

JAR_FILE="backend/target/codingagent-backend-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "🔨 Building backend JAR with Maven..."
    (cd backend && mvn package -DskipTests)
fi

echo "🚀 Starting Spring Boot Backend on http://localhost:8080..."
java -Djava.net.preferIPv4Stack=true -jar "$JAR_FILE" > logs/backend.log 2>&1 &
BACKEND_PID=$!

cleanup() {
    echo ""
    echo "🛑 Shutting down Coding Agent..."
    if [ -n "$BACKEND_PID" ]; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    ./stop.sh
    exit 0
}

trap cleanup SIGINT SIGTERM EXIT

# Wait for backend to become ready
echo -n "⏳ Waiting for backend to start"
for i in {1..30}; do
    if lsof -i :8080 >/dev/null 2>&1; then
        echo " -> Backend ready! (PID: $BACKEND_PID)"
        break
    fi
    sleep 0.5
    echo -n "."
done

echo ""
echo "=========================================================="
echo "🌐 Launching React + TypeScript Dashboard on http://localhost:5173"
echo "   (Press Ctrl+C to stop both backend and frontend)"
echo "=========================================================="

cd frontend
if command -v bun &> /dev/null; then
    bun run dev
elif command -v npm &> /dev/null; then
    npm run dev
else
    echo "Error: Neither bun nor npm found."
    exit 1
fi
