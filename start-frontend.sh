#!/bin/bash
set -e

export PATH="$HOME/.bun/bin:/opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:/usr/local/bin:$PATH"

cd "$(dirname "$0")/frontend"

echo "=================================================="
echo "🌐 Starting Nexcode Frontend (React + Vite)"
echo "   URL: http://localhost:5173"
echo "=================================================="

if command -v npm &> /dev/null; then
    exec npm run dev
elif command -v bun &> /dev/null; then
    exec bun run dev
elif command -v pnpm &> /dev/null; then
    exec pnpm dev
elif command -v yarn &> /dev/null; then
    exec yarn dev
else
    echo "❌ Error: Neither npm, bun, pnpm, nor yarn found. Please install Node.js / Bun."
    exit 1
fi
