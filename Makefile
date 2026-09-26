# AI Harness Hackathon 2026 - Nexcode Autonomous Coding Agent Makefile

SHELL := /bin/bash
export PATH := /opt/homebrew/opt/openjdk/bin:/opt/homebrew/bin:$(PATH)

.PHONY: setup run test clean ui server help

setup:
	@echo "=================================================="
	@echo "📦 Setting up Nexcode dependencies..."
	@echo "=================================================="
	@if [ -d "backend" ]; then \
		echo "Building Spring Boot backend..."; \
		(cd backend && mvn package -DskipTests); \
	fi
	@if [ -d "frontend" ] && command -v npm >/dev/null 2>&1; then \
		echo "Configuring frontend dependencies..."; \
		(cd frontend && npm install --silent 2>/dev/null || true); \
	fi
	@chmod +x run-evaluation.sh start-backend.sh start-frontend.sh start.sh stop.sh 2>/dev/null || true
	@echo "✅ Setup complete. Ready for evaluation."

run:
	@./run-evaluation.sh $(ARGS) $(if $(WORKSPACE),--workspace="$(WORKSPACE)",) $(if $(TASK),--task="$(TASK)",)

test:
	@echo "=================================================="
	@echo "🧪 Running Nexcode real verification test suite..."
	@echo "=================================================="
	@cd backend && mvn test

clean:
	@echo "🧹 Removing generated build artifacts..."
	@rm -rf backend/target
	@rm -rf logs/*.log
	@echo "✅ Clean complete."

ui:
	@./start.sh

server:
	@./start-backend.sh
