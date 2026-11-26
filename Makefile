.PHONY: help up down logs dev db db-down backend frontend test test-backend test-frontend migrate-legacy

help: ## Show available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

up: ## Build and start full stack (Postgres + Backend + Frontend) via Docker
	docker compose up --build -d

down: ## Stop all Docker services (keeps database volume)
	docker compose down

logs: ## Follow Docker Compose logs
	docker compose logs -f

dev: ## Start Postgres in Docker, then run backend + frontend locally with hot reload
	docker compose up -d postgres
	@echo "Starting backend and frontend (Ctrl+C to stop)..."
	@bash -c 'trap "kill 0" EXIT; ./backend/gradlew -p backend run & (cd frontend && npm run dev) & wait'

db: ## Start PostgreSQL only via Docker Compose
	docker compose up -d postgres

db-down: ## Stop all containers
	docker compose down

backend: ## Run Java 25 Javalin backend (port 8080)
	./backend/gradlew -p backend run

frontend: ## Run React + Vite dev server (port 5173)
	cd frontend && npm run dev

test: test-backend test-frontend ## Run all backend and frontend checks

test-backend: ## Run backend unit, HTTP, CSV, and ArchUnit tests
	./backend/gradlew -p backend test --no-daemon

test-frontend: ## Typecheck and build frontend
	cd frontend && npm run build

migrate-legacy: ## Import legacy JSON files from data/ into PostgreSQL
	./backend/gradlew -p backend run -PmainClass=com.imdbselector.infrastructure.adapter.in.cli.LegacyJsonMigrator
