#!/bin/bash

echo "Stopping any running Coding Agent processes..."

# Stop process listening on port 8080 (backend)
PID_8080=$(lsof -ti :8080 2>/dev/null || true)
if [ -n "$PID_8080" ]; then
    echo "Stopping backend process on port 8080 (PID: $PID_8080)..."
    kill -9 $PID_8080 2>/dev/null || true
fi

# Stop process listening on port 5173 (frontend)
PID_5173=$(lsof -ti :5173 2>/dev/null || true)
if [ -n "$PID_5173" ]; then
    echo "Stopping frontend process on port 5173 (PID: $PID_5173)..."
    kill -9 $PID_5173 2>/dev/null || true
fi

echo "✅ Ports 8080 and 5173 are now free."
