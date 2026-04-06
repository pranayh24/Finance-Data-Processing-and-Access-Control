# Finance Dashboard - Backend API

A role-based finance data processing backend built with **Java 17**, **Spring Boot 3.2**, and **PostgreSQL**.

- **GitHub Repository:** https://github.com/pranayh24/Finance-Data-Processing-and-Access-Control
- **Live API (GCP Compute Engine):** http://34.14.206.59:8085/
- **Swagger UI (Live):** http://34.14.206.59:8085/swagger-ui.html
- **Dashboard UI (Static Visualization):** https://pranayh24.github.io/Finance-Data-Processing-and-Access-Control

---

## Deployment

The application is deployed on a **GCP Compute Engine** instance and is publicly accessible.

| Resource        | URL                                                                          |
|-----------------|------------------------------------------------------------------------------|
| Live API        | http://34.14.206.59:8085/                                                    |
| Swagger UI      | http://34.14.206.59:8085/swagger-ui.html                                     |
| API Docs (JSON) | http://34.14.206.59:8085/api-docs                                            |
| GitHub Repo     | https://github.com/pranayh24/Finance-Data-Processing-and-Access-Control      |

The Swagger UI on the live server is fully interactive. You can log in with the seeded credentials below, paste the returned token into the Authorize button, and test every endpoint directly from the browser.

---

## Tech Stack

| Layer       | Technology                             |
|-------------|----------------------------------------|
| Language    | Java 17                                |
| Framework   | Spring Boot 3.2                        |
| Security    | Spring Security + JWT (jjwt 0.12)      |
| Database    | PostgreSQL 15                          |
| ORM         | Spring Data JPA / Hibernate            |
| Migrations  | Flyway                                 |
| Docs        | SpringDoc OpenAPI (Swagger UI)         |
| Testing     | JUnit 5 + MockMvc + H2 (in-memory)     |
| Build       | Maven 3.9                              |
| Hosting     | GCP Compute Engine                     |

---

## Architecture

```
controller  ->  service  ->  repository  ->  PostgreSQL
     |               |
 JWT filter     @PreAuthorize
 (auth)         (role check)
```

Every request passes through a stateless JWT filter before reaching the controllers.
Role enforcement is handled declaratively via `@PreAuthorize` at the method level.
Services own all business logic. Controllers are thin and only handle HTTP concerns.

---

## Roles and Permissions

| Action                            | VIEWER | ANALYST | ADMIN |
|-----------------------------------|--------|---------|-------|
| `GET /api/records`                | ✓      | ✓       | ✓     |
| `GET /api/records/{id}`           | ✓      | ✓       | ✓     |
| `POST /api/records`               | ✗      | ✗       | ✓     |
| `PATCH /api/records/{id}`         | ✗      | ✗       | ✓     |
| `DELETE /api/records/{id}`        | ✗      | ✗       | ✓     |
| `GET /api/dashboard/**`           | ✗      | ✓       | ✓     |
| `GET /api/users`                  | ✗      | ✗       | ✓     |
| `POST /api/users`                 | ✗      | ✗       | ✓     |
| `PATCH /api/users/{id}/role`      | ✗      | ✗       | ✓     |
| `PATCH /api/users/{id}/status`    | ✗      | ✗       | ✓     |

---

## Seeded Credentials

Three users are created automatically by the V2 Flyway migration. Use these to test the live API or local setup immediately:

| Email                  | Password       | Role    |
|------------------------|----------------|---------|
| `admin@finance.com`    | `Admin@1234`   | ADMIN   |
| `analyst@finance.com`  | `Analyst@1234` | ANALYST |
| `viewer@finance.com`   | `Viewer@1234`  | VIEWER  |

Sample financial records across multiple categories and months are also seeded.

---

## Quick Start (Local)

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker (for PostgreSQL)

### 1. Start PostgreSQL

```bash
docker run --name finance-db \
  -e POSTGRES_DB=finance_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:15
```

### 2. Run the application

```bash
./mvnw spring-boot:run
```

Flyway will automatically create tables and seed data on first boot.

The API is available at: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Environment variables (optional overrides)

| Variable      | Default    | Description            |
|---------------|------------|------------------------|
| `DB_USERNAME` | `postgres` | Database username      |
| `DB_PASSWORD` | `postgres` | Database password      |
| `JWT_SECRET`  | (see properties)  | Base64 HMAC-SHA256 key |

---

## API Reference

### Authentication (no token required)

```
POST /api/auth/register   Register a new user
POST /api/auth/login      Login and receive a JWT
```

**Login example (against live server):**
```bash
curl -X POST http://34.14.206.59:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@finance.com","password":"Admin@1234"}'
```

All subsequent requests need: `Authorization: Bearer <token>`

---

### Financial Records

```
GET    /api/records           List records (all roles)
GET    /api/records/{id}      Get one record (all roles)
POST   /api/records           Create record (ADMIN only)
PATCH  /api/records/{id}      Update record (ADMIN only)
DELETE /api/records/{id}      Soft-delete record (ADMIN only)
```

**Filtering (GET /api/records):**

| Param      | Type              | Example            |
|------------|-------------------|--------------------|
| `type`     | INCOME \| EXPENSE | `?type=INCOME`     |
| `category` | string            | `?category=salary` |
| `from`     | yyyy-MM-dd        | `?from=2024-01-01` |
| `to`       | yyyy-MM-dd        | `?to=2024-03-31`   |
| `page`     | int               | `?page=0`          |
| `size`     | int (max 100)     | `?size=20`         |

**Create record example:**
```bash
curl -X POST http://34.14.206.59:8085/api/records \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5000.00,
    "type": "INCOME",
    "category": "Salary",
    "recordDate": "2024-03-01",
    "notes": "March salary"
  }'
```

---

### Dashboard (ANALYST + ADMIN)

```
GET /api/dashboard/summary        Total income, expenses, net balance
GET /api/dashboard/by-category    Breakdown by category and type
GET /api/dashboard/trends         Monthly totals for the last 6 months
GET /api/dashboard/recent?limit=N Most recent N records (max 50)
```

---

### Users (ADMIN only)

```
GET   /api/users               List all users
GET   /api/users/{id}          Get user by ID
POST  /api/users               Create a user
PATCH /api/users/{id}/role     Change a user's role
PATCH /api/users/{id}/status   Activate or deactivate a user
```

---

## Running Tests

```bash
./mvnw test
```

Tests use an H2 in-memory database. No PostgreSQL needed. Flyway is disabled for tests; JPA creates the schema directly.

**Test coverage includes:**
- Auth: register, login, duplicate email, bad credentials
- Records: full CRUD lifecycle, RBAC enforcement per role, validation errors, soft delete visibility
- Dashboard: role-based access, summary data correctness, recent activity limit

---

## Design Decisions and Tradeoffs

**Soft delete over hard delete** - Financial records are never physically removed. The `is_deleted` flag preserves audit history and allows potential recovery. A partial index on `(is_deleted, type, record_date)` keeps queries fast.

**Stateless JWT authentication** - No session state on the server, which makes horizontal scaling trivial. The tradeoff is that token revocation requires either short expiry or a token blacklist (Redis). For this scope, 24-hour expiry with client-side logout is sufficient.

**`@PreAuthorize` over URL-based security** - Method-level security keeps the access rules next to the code they protect, making them easier to audit and change independently of the route structure.

**Patch semantics for updates** - `PATCH /api/records/{id}` only updates fields that are present in the request body. This avoids accidental overwrites and is more honest about intent than a full `PUT`.

**Audit log as a service-layer concern** - Rather than AOP or database triggers, audit entries are written explicitly in service methods. This is more verbose but easier to understand, test, and customize per action.

**Native SQL for trend aggregation** - The monthly trends query uses `DATE_TRUNC` and `TO_CHAR`, which are PostgreSQL-specific. This is the right tradeoff, as expressing a group-by-month aggregation in JPQL would produce inferior SQL. The test profile uses H2 to stay database-agnostic.

**BCrypt cost factor 12** - Slightly slower than the default of 10, but well within acceptable login latency (~300ms). This makes brute-force attacks significantly more expensive.
