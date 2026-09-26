#!/bin/bash
set -e

cd "$(dirname "$0")/frontend"

echo "=================================================="
echo "🌐 Starting Nexcode Frontend (React + Vite)"
echo "   URL: http://localhost:5173"
echo "=================================================="

if command -v bun &> /dev/null; then
    exec bun run dev
elif command -v npm &> /dev/null; then
    exec npm run dev
else
    echo "Error: Neither bun nor npm found. Please install Node.js / Bun."
    exit 1
fi
