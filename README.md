# EventEase

EventEase is a robust, production-oriented event booking platform. It provides a secure backend API, a modern React frontend, and an intelligent AI assistant to help users discover events and manage their bookings.

## 🏗 Architecture & Tech Stack

### Backend
- **Java 21 & Spring Boot 3**
- **Spring Security & JWT**: For authentication and Role-Based Access Control (RBAC).
- **MySQL & Spring Data JPA**: Primary data persistence.
- **Event Service**: A separately runnable Spring Boot application on port 8082 that owns event and ticket catalog writes.
- **Redis**: Caching layer for fast data retrieval.
- **Kafka**: Event-driven architecture for notifications and asynchronous processing.
- **Bucket4j**: Rate-limiting to prevent API abuse.
- **Resilience4j**: Circuit breaker and retry mechanisms for fault tolerance (e.g., payment mock).
- **Spring AI**: Integration with OpenAI for the intelligent EventEase Assistant.
- **Actuator & Prometheus**: Observability and monitoring.
- **Swagger/OpenAPI**: API documentation.

### Frontend
- **React 19 & React Router**: SPA architecture and routing.
- **Vite**: Next-generation frontend tooling.
- **Tailwind CSS v4**: Utility-first styling for a beautiful, responsive UI.
- **Axios**: HTTP client for communicating with the backend APIs.
- **Lucide React**: Modern iconography.

## 🚀 Getting Started

### Prerequisites
- Java 21
- Node.js 18+
- Docker and Docker Compose
- Maven

### Environment Variables

Before starting the backend, copy `.env.example` to `.env` for Docker Compose, or export the variables in your shell. The following values are required or commonly customized:
- `OPENAI_API_KEY`: Optional for the core booking flow; required for the AI assistant.
- `AI_ENABLED`: Set to `true` together with `OPENAI_API_KEY` to enable the AI assistant (defaults to `false`).
- `JWT_SECRET`: Secret key for signing JWTs (required; use a unique value of at least 32 bytes).
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: MySQL connection details.
- `REDIS_HOST`, `REDIS_PORT`: Redis connection details.
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker details.

### Running with Docker Compose (Infrastructure)

Start the complete Dockerized backend and required infrastructure (MySQL, Redis, Kafka, Zookeeper, and the Event Service) using Docker Compose:

```bash
docker-compose up -d
```

The main application starts first and creates/backfills the booking-owned `ticket_inventory` table. The Event Service then starts against the same MySQL database. Existing `events`, `ticket_types`, and booking rows are retained; the old `ticket_types.available_quantity` column is preserved but no longer used as a live balance. The Event Service is the only service that writes event/ticket metadata. The main application is the only service that changes reservation counts.

### Backend Setup

Maven does not load `.env` automatically. In each PowerShell terminal used to start a service, import the existing local `.env` into that process (values are not echoed):

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim().Trim('"').Trim("'"), 'Process')
  }
}
```

Confirm `DB_NAME` and the database portion of `DB_URL` against the existing schema with an authorized database administrator before connecting. Both local applications default to `127.0.0.1:3306`; set the shared `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` in `.env`. Do not use a MySQL root account for application access.

1. Open a terminal in the root directory and import `.env` as shown above.
2. Run the main Spring Boot application using Maven:
   ```bash
   mvn spring-boot:run
   ```
3. The backend will start on `http://localhost:8080` in local Maven mode. Docker Compose exposes it at `http://localhost:8081`.
4. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

### Running the Event Service locally

The Event Service is an independent Spring Boot process, not a separate database. It requires the same verified MySQL schema and shared JWT signing secret. In a second terminal, import the same `.env` values, start the main backend first so its idempotent initializer can prepare `ticket_inventory`, and then start the Event Service:

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
    [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim().Trim('"').Trim("'"), 'Process')
  }
}
```

```powershell
mvn -f event-service\pom.xml spring-boot:run
```

It listens at `http://localhost:8082`. Event and ticket endpoints keep their existing `/api/v1` paths and JSON formats. Public event/ticket reads and administrator-only catalog writes are protected by the same JWT claims and signing secret as the main backend.

### Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
4. The React application will be available at `http://localhost:5173`. During Vite development, the main backend defaults to `http://localhost:8080`; production builds retain `http://localhost:8081` for the Compose host mapping. Event and ticket calls use `VITE_EVENT_API_BASE_URL` (default `http://localhost:8082`):
   ```powershell
   npm run dev
   ```
   If overriding either URL, set `VITE_API_BASE_URL` or `VITE_EVENT_API_BASE_URL` in the frontend process environment before starting Vite.

## 🤖 AI Assistant Setup

The EventEase Assistant is powered by Spring AI. It uses context-aware tool calling to fetch live data directly from the Spring Boot backend without hallucinating.

1. Set `AI_ENABLED=true` and ensure the `OPENAI_API_KEY` environment variable is exported before running the backend.
   - Example (Windows PowerShell): `$env:OPENAI_API_KEY="sk-..."`
   - Example (Bash): `export OPENAI_API_KEY="sk-..."`
2. The AI assistant widget is accessible from any page on the frontend once logged in.
3. It can search for events, check ticket availability, and query your authenticated booking history!

## 📸 Screenshots

> Placeholder for future screenshots of the EventEase frontend and dashboard.

## 🧪 Testing

- **Backend Tests:** Run `mvn test` in the root directory.
- **Event Service Tests:** Run `mvn -f event-service\pom.xml test`.
- **Frontend Build:** Run `npm run build` inside the `/frontend` directory to verify TypeScript compilation.
