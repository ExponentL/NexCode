#!/bin/bash
set -e

# Setup PATH for Java, Node/npm, and Bun cross-platform
export PATH="$HOME/.bun/bin:/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

cd "$(dirname "$0")"

# Ensure EVAL_MODE is explicitly false so the interactive terminal workspace prompt NEVER triggers in web mode
export EVAL_MODE="false"

# Load environment variables if .env exists
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
fi

# Ensure EVAL_MODE remains false for web mode even if present in .env
export EVAL_MODE="false"

mkdir -p logs

echo "======================================================================"
echo "🤖 Nexcode — Autonomous AI Coding Agent (Web Platform)"
echo "======================================================================"

# Verify AI_API_KEY
if [ -z "$AI_API_KEY" ]; then
    echo "⚠️  NOTE: AI_API_KEY is not currently exported in your environment."
    echo "   Remember to export it: export AI_API_KEY=\"your-key\""
fi

# 1. Clean up any leftover processes on ports 8080 or 5173
PID_8080=$(lsof -ti :8080 2>/dev/null || true)
PID_5173=$(lsof -ti :5173 2>/dev/null || true)
if [ -n "$PID_8080" ] || [ -n "$PID_5173" ]; then
    echo "⚠️  Cleaning up previous processes on ports 8080 / 5173..."
    ./stop.sh >/dev/null 2>&1 || true
    sleep 1
fi

JAR_FILE="backend/target/codingagent-backend-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "🔨 Building backend JAR with Maven..."
    (cd backend && mvn package -DskipTests)
fi

# 2. Start Spring Boot Backend in background
echo "🚀 Starting Spring Boot Backend on http://localhost:8080 (0.0.0.0)..."
java -Djava.net.preferIPv4Stack=true -jar "$JAR_FILE" > logs/backend.log 2>&1 &
BACKEND_PID=$!

FRONTEND_PID=""

cleanup() {
    echo ""
    echo "🛑 Shutting down Nexcode agent processes..."
    if [ -n "$FRONTEND_PID" ]; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
    if [ -n "$BACKEND_PID" ]; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    ./stop.sh >/dev/null 2>&1 || true
    exit 0
}

trap cleanup SIGINT SIGTERM EXIT

# 3. Wait for backend to become ready and verify process is alive
echo -n "⏳ Waiting for backend to start"
BACKEND_READY=false
for i in {1..50}; do
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo ""
        echo "❌ Error: Spring Boot backend process exited unexpectedly!"
        echo "--- Backend Log Tail ---"
        tail -n 25 logs/backend.log 2>/dev/null || true
        exit 1
    fi
    if curl -s http://127.0.0.1:8080/api/workspaces/current >/dev/null 2>&1 || lsof -i :8080 >/dev/null 2>&1; then
        BACKEND_READY=true
        echo " -> Backend ready! (PID: $BACKEND_PID)"
        break
    fi
    sleep 0.5
    echo -n "."
done

if [ "$BACKEND_READY" != "true" ]; then
    echo ""
    echo "❌ Error: Backend failed to respond within 25 seconds."
    echo "--- Backend Log Tail ---"
    tail -n 30 logs/backend.log 2>/dev/null || true
    exit 1
fi

# 4. Check Frontend Package Manager & Dependencies
if [ ! -d "frontend" ]; then
    echo "❌ Error: frontend directory not found."
    exit 1
fi

PM=""
PM_CMD=""
if command -v npm &> /dev/null; then
    PM="npm"
    PM_CMD="npm run dev"
elif command -v bun &> /dev/null; then
    PM="bun"
    PM_CMD="bun run dev"
elif command -v pnpm &> /dev/null; then
    PM="pnpm"
    PM_CMD="pnpm dev"
elif command -v yarn &> /dev/null; then
    PM="yarn"
    PM_CMD="yarn dev"
else
    echo "❌ Error: No JavaScript package manager found (npm, bun, pnpm, yarn)."
    echo "   Please install Node.js / npm or Bun."
    exit 1
fi

if [ ! -d "frontend/node_modules" ]; then
    echo "📦 Installing frontend dependencies with $PM..."
    (cd frontend && $PM install)
fi

# 5. Launch React Frontend
echo "🚀 Starting React Frontend with '$PM_CMD'..."
(cd frontend && $PM_CMD) > logs/frontend.log 2>&1 &
FRONTEND_PID=$!

# 6. Verify that frontend process actually starts and port 5173 is accessible
echo -n "⏳ Waiting for React frontend to start"
FRONTEND_READY=false
for i in {1..30}; do
    if ! kill -0 "$FRONTEND_PID" 2>/dev/null; then
        echo ""
        echo "❌ Error: Frontend process ($PM_CMD) exited immediately!"
        echo "--- Frontend Log Tail ---"
        tail -n 25 logs/frontend.log 2>/dev/null || true
        exit 1
    fi
    if curl -s http://127.0.0.1:5173 >/dev/null 2>&1 || lsof -i :5173 >/dev/null 2>&1; then
        FRONTEND_READY=true
        echo " -> Frontend ready! (PID: $FRONTEND_PID)"
        break
    fi
    sleep 0.5
    echo -n "."
done

if [ "$FRONTEND_READY" != "true" ]; then
    echo ""
    echo "❌ Error: Frontend failed to bind port 5173 within 15 seconds."
    echo "--- Frontend Log Tail ---"
    tail -n 25 logs/frontend.log 2>/dev/null || true
    exit 1
fi

# 7. Detect local network IP for cross-device access
LOCAL_IP=""
if command -v ipconfig &>/dev/null; then
    LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)
fi
if [ -z "$LOCAL_IP" ] && command -v hostname &>/dev/null; then
    LOCAL_IP=$(hostname -I 2>/dev/null | awk '{print $1}' || true)
fi

# 8. Print actual verified URLs clearly
echo ""
echo "======================================================================"
echo "  🎉 Nexcode Dashboard is LIVE and verified running!"
echo "  🌐 Web Dashboard: http://localhost:5173"
if [ -n "$LOCAL_IP" ] && [ "$LOCAL_IP" != "127.0.0.1" ]; then
echo "  📱 Network URL:   http://${LOCAL_IP}:5173"
fi
echo "  ⚙️  Backend API:   http://localhost:8080"
if [ -n "$LOCAL_IP" ] && [ "$LOCAL_IP" != "127.0.0.1" ]; then
echo "  ⚙️  Backend Net:   http://${LOCAL_IP}:8080"
fi
echo "======================================================================"
echo "  • Open your browser to http://localhost:5173"
echo "  • Select or provide your target repository in the web interface"
echo "  • Enter your coding prompt / issue to execute live"
echo "  • Press Ctrl+C in this terminal to stop both servers cleanly"
echo "======================================================================"
echo ""

# 9. Automatically open browser if practical
if [ -z "$CI" ] && [ -z "$SSH_CLIENT" ] && [ -z "$SSH_TTY" ]; then
    if command -v open >/dev/null 2>&1; then
        (sleep 1 && open "http://localhost:5173") >/dev/null 2>&1 &
    elif command -v xdg-open >/dev/null 2>&1; then
        (sleep 1 && xdg-open "http://localhost:5173") >/dev/null 2>&1 &
    fi
fi

# Keep script running to monitor processes and handle graceful shutdown
wait "$FRONTEND_PID" "$BACKEND_PID"
