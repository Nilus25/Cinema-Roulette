.PHONY: help db db-down backend frontend test test-backend test-frontend migrate-legacy

help: ## Show available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

db: ## Start PostgreSQL via Docker Compose
	docker compose up -d

db-down: ## Stop PostgreSQL container
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
