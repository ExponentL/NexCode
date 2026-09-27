#!/bin/bash
set -e

# Setup PATH for Java, Node/npm, and Bun cross-platform
export PATH="$HOME/.bun/bin:/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

cd "$(dirname "$0")"

# Load environment variables if .env exists
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
fi

mkdir -p logs

echo "======================================================================"
echo "🤖 Nexcode — Autonomous AI Coding Agent Harness"
echo "======================================================================"

# Verify AI_API_KEY
if [ -z "$AI_API_KEY" ]; then
    echo "⚠️  NOTE: AI_API_KEY is not currently exported in your environment."
    echo "   Remember to export it: export AI_API_KEY=\"your-key\""
fi

# Clean up any leftover processes on ports 8080 or 5173
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
    echo "🛑 Shutting down Nexcode agent processes..."
    if [ -n "$BACKEND_PID" ]; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    ./stop.sh >/dev/null 2>&1 || true
    exit 0
}

trap cleanup SIGINT SIGTERM EXIT

# Wait for backend to become ready
echo -n "⏳ Waiting for backend to start"
for i in {1..40}; do
    if curl -s http://localhost:8080/api/workspaces/current >/dev/null 2>&1 || lsof -i :8080 >/dev/null 2>&1; then
        echo " -> Backend ready! (PID: $BACKEND_PID)"
        break
    fi
    sleep 0.5
    echo -n "."
done

echo ""
echo "======================================================================"
echo "  🚀 Nexcode Dashboard is running!"
echo "  🌐 Web Dashboard: http://localhost:5173"
echo "  ⚙️  Backend API:   http://localhost:8080"
echo "======================================================================"
echo "  • Open your browser to http://localhost:5173"
echo "  • Select or clone your target repository in the dashboard"
echo "  • Provide coding tasks / issues to execute"
echo "  • Press Ctrl+C in this terminal to stop both servers"
echo "======================================================================"
echo ""

# Automatically open browser if practical
if [ -z "$CI" ] && [ -z "$SSH_CLIENT" ] && [ -z "$SSH_TTY" ]; then
    if command -v open >/dev/null 2>&1; then
        (sleep 1.5 && open "http://localhost:5173") >/dev/null 2>&1 &
    elif command -v xdg-open >/dev/null 2>&1; then
        (sleep 1.5 && xdg-open "http://localhost:5173") >/dev/null 2>&1 &
    fi
fi

cd frontend
if command -v npm &> /dev/null; then
    npm run dev
elif command -v bun &> /dev/null; then
    bun run dev
elif command -v pnpm &> /dev/null; then
    pnpm dev
elif command -v yarn &> /dev/null; then
    yarn dev
else
    echo "❌ Error: No JavaScript package manager found (npm, bun, pnpm, yarn)."
    echo "   Please install Node.js / npm or Bun."
    exit 1
fi
