<div align="center">

# rally-auth

**Identity & Access Management microservice for the RallyDeals platform.**

[![Build Status](https://img.shields.io/github/actions/workflow/status/RallyDeals/rally-auth/docker-publish.yml?style=flat-square&label=Build)](https://github.com/RallyDeals/rally-auth/actions)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apachekafka&logoColor=white)
[![Docker](https://img.shields.io/badge/Docker-Hub-2496ED?style=flat-square&logo=docker&logoColor=white)](https://hub.docker.com/r/medhatdh/rally-auth)

[Overview](#overview) | [Architecture](#architecture) | [Getting Started](#getting-started) | [Configuration](#configuration) | [API Reference](#api-reference) | [Docker](#docker-image)

</div>

---

## Overview

rally-auth is the identity backbone of the RallyDeals platform. It handles user registration, authentication, JWT-based session management, OTP-verified email flows, profile management, and administrative user controls.

The service is designed to run behind an API gateway that validates JWTs and injects user identity via trusted HTTP headers (`X-User-Id`, `X-User-Name`, `X-User-Role`). It publishes domain events through a **transactional outbox pattern** to Apache Kafka, enabling asynchronous notification delivery without direct service-to-service coupling.

### Key capabilities

- **Registration & login** with BCrypt password hashing and RS256 JWT access/refresh token pairs
- **Email OTP flows** for account verification and password reset with rate-limiting and retry protection
- **Profile management** including avatar upload and partial-profile updates
- **Admin user management** with search, role changes, ban/activate controls, and batch user resolution
- **Transactional outbox** for reliable Kafka event publishing (`User.Registered`, `User.EmailVerificationRequested`, `User.PasswordResetRequested`)
- **Observability** with OpenTelemetry tracing, Loki log shipping, and structured JSON logging

## Architecture

```text
                  +-----------+
                  |  Gateway  |  validates JWT, injects X-User-* headers
                  +-----+-----+
                        |
                        v
               +--------+--------+
               |   rally-auth    |---[outbox]--> Kafka (user.events)
               +--------+--------+
                        |
                        v
               +--------+--------+
               |   PostgreSQL    |
               |    (auth_db)    |
               +-----------------+
```

> [!NOTE]
> rally-auth does **not** consume any Kafka topics. It is a pure event producer. The Notification Service consumes `user.events` to send emails.

### Event-driven email flow

1. User triggers an action (register, request reset, etc.)
2. OTP is generated and stored in the database
3. An outbox event is written with the OTP payload **encrypted** via AES-256
4. The `OutboxRelay` polls the outbox table and publishes to Kafka
5. Notification Service decrypts the OTP from the event payload and sends the email

## Getting Started

### Prerequisites

- **Java 21** (Temurin recommended)
- **Maven 3.9+** (or use the included `mvnw` wrapper)
- **Docker & Docker Compose** (for PostgreSQL and Kafka)
- **GitHub Packages access** (to pull `rally-common`)

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts:
- PostgreSQL 16 on `localhost:5430` (database: `auth_db`)
- Kafka on `localhost:9092`
- Kafka UI on `localhost:8080`

### 2. Configure environment

```bash
cp .env.example .env
```

The defaults work with the Docker Compose setup. For GitHub Packages authentication, set your credentials:

```bash
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=your-personal-access-token
```

> [!IMPORTANT]
> You must have access to the `RallyDeals/rally-common` GitHub repository to pull the shared library. Generate a personal access token with `read:packages` scope.

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The service starts on port `8082` by default (configurable via `SERVER_PORT`).

### 4. Verify

- **Health check:** `GET http://localhost:8082/actuator/health`
- **Swagger UI:** `http://localhost:8082/swagger-ui.html`
- **API docs:** `http://localhost:8082/api-docs`

### Development seed data

The V2 Flyway migration seeds 11 test users across all roles. All passwords are `SeedPass123!`.

| Email | Role | Status |
|---|---|---|
| `a1@rally.local` | ADMIN | Active, verified |
| `admin.locked@rally.local` | ADMIN | Disabled |
| `s1@rally.local` | SELLER | Active, verified |
| `u1@rally.local` | BUYER | Active, verified |

## Configuration

All configuration is via environment variables (with sensible defaults). See [`.env.example`](.env.example) for the full reference.

### Core settings

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8082` | Application port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5430` | PostgreSQL port |
| `DB_NAME` | `auth_db` | Database name |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |

### Security settings

| Variable | Default | Description |
|---|---|---|
| `ACCESS_TOKEN_SECONDS` | `900` | JWT access token lifetime (15 min) |
| `REFRESH_TOKEN_DAYS` | `30` | Refresh token lifetime |
| `OTP_LENGTH` | `6` | OTP code length |
| `OTP_EXPIRATION_MINUTES` | `3` | OTP validity window |
| `OTP_MAX_ATTEMPTS` | `5` | Max OTP verification attempts |
| `OTP_RESEND_COOLDOWN_SECONDS` | `60` | Cooldown between OTP resends |
| `PASSWORD_MIN_LENGTH` | `8` | Minimum password length |
| `PASSWORD_MAX_LENGTH` | `64` | Maximum password length |
| `JWT_PRIVATE_KEY` | _(embedded)_ | RSA private key for JWT signing (PKCS8 base64) |
| `JWT_PUBLIC_KEY` | _(embedded)_ | RSA public key for JWT verification |

> [!WARNING]
> The embedded JWT keys are for development only. Always provide your own RSA key pair in production.

## API Reference

### Auth endpoints (`/auth`)

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/auth/register` | Register a new user | Public |
| `POST` | `/auth/login` | Authenticate and receive tokens | Public |
| `POST` | `/auth/verify-email` | Verify email with OTP | Public |
| `POST` | `/auth/verify-email-otp` | Pre-check verification OTP | Public |
| `POST` | `/auth/resend-verification-otp` | Resend verification OTP | Public |
| `POST` | `/auth/forgot-password` | Request password reset OTP | Public |
| `POST` | `/auth/verify-reset-otp` | Pre-check reset OTP | Public |
| `POST` | `/auth/reset-password` | Set new password with OTP | Public |
| `POST` | `/auth/refresh` | Rotate refresh token | Public |
| `POST` | `/auth/logout` | Revoke refresh token | Gateway |
| `POST` | `/auth/change-password` | Change password | Gateway |
| `GET` | `/auth/me` | Get own profile | Gateway |
| `PATCH` | `/auth/me` | Update own profile | Gateway |
| `POST` | `/auth/me/avatar` | Upload avatar image | Gateway |

### User management endpoints (`/users`)

All user management endpoints require admin identity via gateway headers.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/users` | List users with search, type, and status filters (paginated) |
| `GET` | `/users/sellers` | List seller accounts (paginated) |
| `GET` | `/users/{id}` | Get user profile by ID |
| `PATCH` | `/users/{id}` | Update user profile |
| `PATCH` | `/users/{id}/ban` | Ban user (revokes all tokens) |
| `PATCH` | `/users/{id}/activate` | Activate user |
| `PATCH` | `/users/{id}/role` | Change user role |
| `GET` | `/users/{id}/roles` | Get user's role |
| `GET` | `/users/sellers/{id}` | Get single seller profile |
| `GET` | `/users/batch` | Batch-resolve users by UUID list |

> [!TIP]
> Open Swagger UI at `/swagger-ui.html` to explore the full request/response schemas and try requests interactively.

### User roles

| Role | Description |
|---|---|
| `BUYER` | Default role. Can browse, join deals, and place orders |
| `SELLER` | Can create and manage deals |
| `ADMIN` | Full platform administration access |

## Docker Image

The service builds to a Docker image via [Jib](https://github.com/GoogleContainerTools/jib) (no Dockerfile required).

### Build locally

```bash
./mvnw -B compile jib:dockerBuild -DskipTests
```

This creates the image `medhatdh/rally-auth:latest` based on `eclipse-temurin:21-jre-alpine`.

### CI/CD

The GitHub Actions workflow (`.github/workflows/docker-publish.yml`) automatically builds and pushes to Docker Hub on pushes to `develop`:

- `medhatdh/rally-auth:latest`
- `medhatdh/rally-auth:v<run_number>`

## Observability

- **Distributed tracing** via OpenTelemetry (Micrometer bridge) with OTLP export
- **Structured JSON logging** via Logstash encoder (plain text in local/dev profiles)
- **Log shipping** to Loki via `loki-logback-appender`
- **Correlation IDs** propagated via `X-Correlation-Id` header, MDC, and W3C Baggage
- **Health endpoints** at `/actuator/health` with detailed component status

## Project Structure

```text
src/main/java/com/rally/auth/
├── AuthApplication.java          # Spring Boot entry point
├── api/                          # REST controllers
│   ├── AuthController.java       # Auth & self-service endpoints
│   └── UserController.java       # Admin user management endpoints
├── config/                       # Application configuration
│   ├── AppProperties.java        # Typed config properties
│   ├── CryptoConfig.java         # BCrypt + JWT RSA key config
│   ├── OutboxRelayProperties.java
│   └── WebConfig.java            # MVC configuration
├── domain/                       # JPA entities
│   ├── otp/                      # EmailOtp, OtpPurpose
│   ├── token/                    # RefreshToken
│   └── user/                     # User, Role
├── dto/                          # Request/response DTOs (records)
├── exception/                    # Domain exceptions
├── filters/                      # CorrelationIdFilter
├── messaging/
│   ├── contract/                 # Message headers & event types
│   └── outbox/                   # OutboxEventWriter, OutboxMessage entity
├── relay/                        # OutboxPublisher, OutboxRelay (polling)
├── repository/                   # Spring Data JPA repositories
├── security/                     # JwtTokenService, RefreshTokenFactory, OtpEncryptor
└── service/                      # Business logic services

src/main/resources/
├── application.properties        # All configuration
├── logback-spring.xml            # Logging config (local vs JSON)
└── db/migration/                 # Flyway SQL migrations
    ├── V1__initial_schema.sql    # Tables: users, tokens, email_otps, outbox_messages
    └── V2__seed_dev_users.sql    # Development seed data
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Temurin) |
| Framework | Spring Boot 4.1.0 |
| Database | PostgreSQL 16 (Flyway migrations) |
| Messaging | Apache Kafka (transactional outbox pattern) |
| Auth | JWT (RS256 via jjwt 0.12.6), BCrypt, AES-256 OTP encryption |
| API Docs | SpringDoc OpenAPI 2.8.5 |
| Observability | OpenTelemetry, Micrometer, Loki, Logstash |
| Container | Jib (eclipse-temurin:21-jre-alpine) |
| Shared Library | [rally-common](https://github.com/RallyDeals/rally-common) 0.3.0 |

## Related Services

rally-auth is part of the [RallyDeals](https://github.com/RallyDeals) platform:

| Service | Description |
|---|---|
| [rally-common](https://github.com/RallyDeals/rally-common) | Shared exceptions, error handling, and DTOs |
| [rally-gateway](https://github.com/RallyDeals/rally-gateway) | API gateway with JWT validation |
| [rally-catalog](https://github.com/RallyDeals/rally-catalog) | Product catalog management |
| [rally-deal](https://github.com/RallyDeals/rally-deal) | Deal lifecycle management |
| [rally-inventory](https://github.com/RallyDeals/rally-inventory) | Inventory tracking |
| [rally-order](https://github.com/RallyDeals/rally-order) | Order processing |
| [rally-payment](https://github.com/RallyDeals/rally-payment) | Payment processing |
| [rally-notification](https://github.com/RallyDeals/rally-notification) | Email & push notifications |
| [rally-participation](https://github.com/RallyDeals/rally-participation) | Deal participation & invites |

## Troubleshooting

> [!TIP]
> If the application fails to start, check that:
> - PostgreSQL is running on the expected port (`docker compose ps`)
> - The `auth_db` database exists and is accessible
> - Kafka broker is reachable at `localhost:9092`
> - GitHub Packages credentials are configured (for `rally-common` dependency)
> - JWT keys are configured (embedded defaults work for development)

> [!NOTE]
> For issues with the `rally-common` dependency, ensure your `~/.m2/settings.xml` includes the GitHub Packages server configuration. The included `settings.xml` can be copied to your Maven home.
