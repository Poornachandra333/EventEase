# EventEase

EventEase is a robust, production-oriented event booking platform. It provides a secure backend API, a modern React frontend, and an intelligent AI assistant to help users discover events and manage their bookings.

## 🏗 Architecture & Tech Stack

### Backend
- **Java 21 & Spring Boot 3**
- **Spring Security & JWT**: For authentication and Role-Based Access Control (RBAC).
- **MySQL & Spring Data JPA**: Primary data persistence.
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

Start the complete Dockerized backend and required infrastructure (MySQL, Redis, Kafka, Zookeeper) using Docker Compose:

```bash
docker-compose up -d
```

### Backend Setup

1. Define `JWT_SECRET` and database variables, then open a terminal in the root directory.
2. Run the Spring Boot application using Maven:
   ```bash
   mvn spring-boot:run
   ```
3. The backend will start on `http://localhost:8080` in local Maven mode. Docker Compose exposes it at `http://localhost:8081`.
4. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

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
4. The React application will be available at `http://localhost:5173`.
   When running the backend directly with Maven, start the frontend with
   `VITE_API_BASE_URL=http://localhost:8080 npm run dev` so it uses the local backend port.

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
- **Frontend Build:** Run `npm run build` inside the `/frontend` directory to verify TypeScript compilation.
