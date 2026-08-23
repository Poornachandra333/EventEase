# EventEase — Event Booking Platform

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-brightgreen?style=for-the-badge&logo=springboot" />
  <img src="https://img.shields.io/badge/Apache_Kafka-3.x-black?style=for-the-badge&logo=apachekafka" />
  <img src="https://img.shields.io/badge/Redis-7.x-red?style=for-the-badge&logo=redis" />
  <img src="https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker" />
</p>

A **production-grade** event ticketing and booking platform built with Spring Boot 3.3.5 + Java 21.  
Demonstrates real-world backend engineering patterns: event-driven architecture, distributed caching, circuit breakers, idempotent payments, rate limiting, and observability.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────┐
│               REST API Layer                │
│  (JWT Auth + Rate Limiting + OpenAPI Docs)  │
├──────────────┬──────────────────────────────┤
│  Controllers │  EventController             │
│              │  VenueController             │
│              │  BookingController           │
│              │  PaymentController           │
│              │  TicketTypeController        │
│              │  AuthController / UserCtrl   │
├──────────────┴──────────────────────────────┤
│             Service Layer                   │
│  EventService  │ VenueService               │
│  BookingService│ PaymentService             │
│  TicketService │ AuthService / UserService  │
├──────────────────────────────────────────────┤
│         Infrastructure Layer                │
│  MySQL 8.0    │ Redis 7 (Cache)             │
│  Apache Kafka │ Resilience4j                │
│  Bucket4j     │ Spring Actuator+Prometheus  │
└──────────────────────────────────────────────┘
```

---

## 🚀 Production-Grade Features

### 1. 🔐 JWT Authentication & RBAC
- Stateless JWT authentication with `HS512` algorithm
- Two roles: `ADMIN` (manage events/venues) and `USER` (book/cancel tickets)
- Custom entry points for 401 Unauthorized and 403 Forbidden

### 2. 📨 Apache Kafka — Event-Driven Notifications
- **3 Kafka topics** auto-created on startup: `booking-created`, `booking-cancelled`, `payment-processed`
- `BookingEventProducer` publishes domain events asynchronously after each booking/payment action
- `BookingEventConsumer` consumes events to simulate email/SMS dispatch (notification service hook)
- Booking reference used as **Kafka message key** to guarantee per-booking ordering

### 3. ⚡ Redis Caching
- `@Cacheable` on `getEventById` (15 min TTL) and `getVenueById` / `getAllVenues` (30 min TTL)
- `@CacheEvict` on all mutating operations to maintain cache consistency
- JSON serialization with `GenericJackson2JsonRedisSerializer` + Java time module support

### 4. 🛡️ Rate Limiting (Bucket4j — Token Bucket Algorithm)
- Per-client rate limiting: authenticated users (20 req/min), anonymous users (10 req/min by IP)
- Returns `HTTP 429 Too Many Requests` with `Retry-After` and `X-Rate-Limit-Remaining` headers
- Skips actuator and Swagger endpoints automatically

### 5. 💳 Mock Payment Integration (Razorpay-style)
- `PENDING_PAYMENT → CONFIRMED / CANCELLED` booking lifecycle
- **Idempotency key** pattern: `orderId = PAY-{bookingReference}-{uuid}` prevents duplicate charges
- 90% mock success rate to simulate real gateway behavior with latency simulation
- Full refund flow: `processRefund()` marks payment as `REFUNDED`

### 6. 🔌 Resilience4j Circuit Breaker
- Circuit breaker wraps all payment gateway calls
- **Opens** after 50% failure rate in last 10 calls (10 second slow-call duration threshold)
- **Fallback**: returns `GATEWAY_UNAVAILABLE` response gracefully instead of crashing
- 3-attempt **retry** with 500ms wait on transient failures

### 7. 📊 Spring Actuator + Prometheus
- Health, info, metrics endpoints at `/actuator/**`
- `/actuator/health` and `/actuator/info` are public; rest require ADMIN
- Prometheus metrics at `/actuator/prometheus` for Grafana dashboards
- Circuit breaker metrics exposed via Micrometer

---

## 🗄️ Domain Model

```
User ─────< Booking >──────── BookingItem ──── TicketType
                │                                    │
                │                                    │
             Payment                               Event ──── Venue
```

| Entity       | Key Fields                                                       |
|-------------|------------------------------------------------------------------|
| `User`       | id, name, email, password (BCrypt), role (USER/ADMIN)           |
| `Event`      | id, title, category, eventDate, status (DRAFT/PUBLISHED/etc.)  |
| `Venue`      | id, name, city, address, capacity                               |
| `TicketType` | id, name, price, totalQuantity, availableQuantity               |
| `Booking`    | id, bookingReference (EE-XXXXXXXX), status, totalAmount         |
| `BookingItem`| id, quantity, priceAtBookingTime                                |
| `Payment`    | id, orderId (idempotency key), gatewayPaymentId, status, amount |

---

## 📋 API Endpoints

### Auth
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/auth/register` | Public | Register new user |
| POST | `/api/v1/auth/login` | Public | Login, returns JWT |

### Events
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/v1/events` | Public | Search/filter events |
| GET | `/api/v1/events/{id}` | Public | Get event by ID (cached) |
| POST | `/api/v1/events` | ADMIN | Create event |
| PUT | `/api/v1/events/{id}` | ADMIN | Update event |
| DELETE | `/api/v1/events/{id}` | ADMIN | Delete event |

### Venues
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/v1/venues` | Authenticated | List all venues (cached) |
| GET | `/api/v1/venues/{id}` | Authenticated | Get venue (cached) |
| POST | `/api/v1/venues` | ADMIN | Create venue |
| PUT | `/api/v1/venues/{id}` | ADMIN | Update venue |
| DELETE | `/api/v1/venues/{id}` | ADMIN | Delete venue |

### Bookings
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/bookings` | USER/ADMIN | Create booking (PENDING_PAYMENT) |
| GET | `/api/v1/bookings/my` | USER/ADMIN | My bookings (paginated) |
| GET | `/api/v1/bookings/{id}` | USER/ADMIN | Get booking |
| PUT | `/api/v1/bookings/{id}/cancel` | USER/ADMIN | Cancel + restore tickets |

### Payments
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/v1/payments/bookings/{id}/pay` | USER/ADMIN | Pay for booking (circuit-breaker) |
| POST | `/api/v1/payments/bookings/{id}/refund` | USER/ADMIN | Refund paid booking |

### Ticket Types
| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| GET | `/api/v1/events/{id}/ticket-types` | Public | List ticket types |
| POST | `/api/v1/events/{id}/ticket-types` | ADMIN | Create ticket type |
| PUT | `/api/v1/ticket-types/{id}` | ADMIN | Update ticket type |
| DELETE | `/api/v1/ticket-types/{id}` | ADMIN | Delete ticket type |

---

## ⚙️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Security | Spring Security + JWT (JJWT 0.12) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8.0 |
| Cache | Redis 7 (Spring Cache) |
| Messaging | Apache Kafka 3.x |
| Rate Limiting | Bucket4j 8.x (Token Bucket) |
| Resilience | Resilience4j (Circuit Breaker + Retry) |
| Observability | Spring Actuator + Micrometer + Prometheus |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3.9 |
| Containerization | Docker + Docker Compose |

---

## 🐳 Running Locally

### Prerequisites
- Java 21+
- Docker + Docker Compose
- Maven 3.9+

### Step 1: Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- **MySQL** on port `3306`
- **Redis** on port `6379`
- **Zookeeper** on port `2181`
- **Kafka** on port `9092`

### Step 2: Run the Application

```bash
mvn spring-boot:run
```

### Step 3: Access Services

| Service | URL |
|---------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |
| Prometheus Metrics | http://localhost:8080/actuator/prometheus |

---

## 📁 Project Structure

```
src/main/java/com/eventease/
├── config/          # Kafka, Redis, Security, OpenAPI configs
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities
├── enums/           # BookingStatus, PaymentStatus, Role, EventStatus
├── event/           # Kafka event DTOs + Producer + Consumer
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── mapper/          # Entity ↔ DTO mappers
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, rate limit filter, UserDetails
├── service/         # Service interfaces
│   └── impl/        # Service implementations
└── util/            # Utility classes
```

---

## 🔑 Key Design Patterns Used

| Pattern | Where Applied |
|---------|--------------|
| **Repository Pattern** | All data access via `JpaRepository` |
| **DTO Pattern** | Separate request/response DTOs |
| **Event-Driven** | Kafka events for booking/payment notifications |
| **Token Bucket** | Rate limiting via Bucket4j |
| **Circuit Breaker** | Resilience4j on payment gateway calls |
| **Idempotency Key** | `orderId` prevents duplicate payment charges |
| **Atomic Updates** | SQL `UPDATE WHERE availableQuantity >= quantity` prevents overselling |
| **Strategy Pattern** | `PaymentGatewayService` interface for swappable gateways |

---

## 🌱 Data Seeding

On startup, `DataSeeder` auto-creates:
- 1 ADMIN user: `admin@eventease.com` / `Admin@1234`
- 1 regular USER: `user@eventease.com` / `User@1234`
- Sample venues and events

---

*Built as a portfolio project demonstrating production-grade Java backend engineering for Amazon/FAANG interview preparation.*
