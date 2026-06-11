# Order Management Engine

Order Management Engine is a Spring Boot service that implements a complete order lifecycle: creation, controlled state transitions, scheduled PENDING→PROCESSING promotion, optional status filtering, caching, and environment-driven configuration.

Prerequisites
- Java 21
- Maven 3.8+

Build
- mvn -f Order-engine clean package -DskipTests

Run
- mvn -f Order-engine spring-boot:run
or
- java -jar Order-engine/target/order-engine-0.0.1-SNAPSHOT.jar

Tests
- mvn -f Order-engine test

Quick notes
- The service uses an embedded H2 database by default (runtime scope). Production configuration should point to a managed database via application properties or environment variables.
- OpenAPI UI (springdoc) is available when the app is running: http://localhost:8080/swagger-ui/index.html
- H2 console is available when using embedded H2: http://localhost:8080/h2-console/
- Scheduler delay and other operational settings are externalized via configuration. See src/main/resources/application*.properties for defaults.

Agentic AI Problem Statement

This repository includes an "Agentic" problem statement used during development to preserve reasoning and decisions across sessions. The following development skills were used to keep work focused and verifiable:

## Architect
- Inspect codebase and identify gaps in entities, DTOs, services, controllers, scheduling, and tests. Aim for end-to-end lifecycle restoration rather than isolated fixes.

## Review
- Verify production-facing behavior: valid order creation, controlled transitions, scheduled promotions, filtering, logging, caching, and environment-driven configuration.

## Remember
- Preserve decisions in configuration, tests, logging, and documentation so future sessions do not need to rediscover them.

## Imprint
- Keep implementation patterns consistent: service-layer behavior, thin controllers, structured exception handling, SLF4J logging, and environment-backed configuration.

Contact
- Maintain repository README and project metadata for more details.

Test Case Samples

Unit tests (service layer)
- createValidOrder_shouldReturnCreated: POST /orders with valid payload -> 201 and status PENDING
- createInvalidAmount_shouldReturn400: amount <= 0 -> 400 validation error with structured JSON
- invalidTransition_shouldReturn409: illegal state change -> 409 Conflict

Integration tests (H2 + scheduler)
- scheduledPromotion_shouldBecomeProcessing: create order with scheduledProcessingDelaySeconds=1, wait, then GET -> status PROCESSING
- idempotentStatusUpdate: PATCH same status twice -> both return 200 and same resource

Quick curl examples
- Create order:
  curl -s -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"amount":100,"currency":"USD"}'
  # expect 201 and "status":"PENDING"

- Change status:
  curl -s -X PATCH http://localhost:8080/orders/{id}/status -H "Content-Type: application/json" -d '{"status":"CANCELLED"}'
  # expect 200 or 409 if transition invalid


  # API Contracts

## Base URL

```http
http://localhost:8080
```

---

# Order Lifecycle

```text
 PENDING
   |
   v
  PROCESSING
  /          \
 v            v
COMPLETED  CANCELLED
```

Valid transitions:

| Current Status | Allowed Next Status   |
| -------------- | --------------------- |
| PENDING        | PROCESSING, CANCELLED |
| PROCESSING     | COMPLETED, CANCELLED  |
| COMPLETED      | Not Allowed           |
| CANCELLED      | Not Allowed           |

---

# 1. Create Order

Creates a new order in `PENDING` status.

## Endpoint

```http
POST /orders
```

## Request Headers

```http
Content-Type: application/json
```

## Request Body

```json
{
  "customerId": "8b43b5c0-16cb-4c4e-a706-0f8d59f1a1a9",
  "amount": 100.50,
  "currency": "USD",
  "metadata": {
    "source": "WEB",
    "channel": "MOBILE"
  },
  "scheduledProcessingDelaySeconds": 60
}
```

## Validation Rules

| Field                           | Required | Validation             |
| ------------------------------- | -------- | ---------------------- |
| amount                          | Yes      | Must be greater than 0 |
| currency                        | Yes      | Cannot be blank        |
| customerId                      | No       | Valid UUID             |
| scheduledProcessingDelaySeconds | No       | Positive integer       |

## Success Response

### HTTP 201 Created

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerId": "8b43b5c0-16cb-4c4e-a706-0f8d59f1a1a9",
  "amount": 100.50,
  "currency": "USD",
  "status": "PENDING",
  "createdAt": "2026-06-11T12:00:00Z",
  "updatedAt": "2026-06-11T12:00:00Z",
  "metadata": {
    "source": "WEB",
    "channel": "MOBILE"
  }
}
```

## Error Responses

### HTTP 400 Bad Request

```json
{
  "timestamp": "2026-06-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Amount must be greater than zero",
  "path": "/orders",
  "code": "VALIDATION_ERROR"
}
```

---

# 2. Get Order By ID

Fetches a specific order.

## Endpoint

```http
GET /orders/{id}
```

## Path Parameters

| Parameter | Type |
| --------- | ---- |
| id        | UUID |

## Success Response

### HTTP 200 OK

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerId": "8b43b5c0-16cb-4c4e-a706-0f8d59f1a1a9",
  "amount": 100.50,
  "currency": "USD",
  "status": "PROCESSING",
  "createdAt": "2026-06-11T12:00:00Z",
  "updatedAt": "2026-06-11T12:05:00Z"
}
```

## Not Found

### HTTP 404 Not Found

```json
{
  "timestamp": "2026-06-11T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found",
  "path": "/orders/123",
  "code": "ORDER_NOT_FOUND"
}
```

---

# 3. Get Orders

Returns paginated orders.

## Endpoint

```http
GET /orders
```

## Query Parameters

| Parameter | Required | Description            |
| --------- | -------- | ---------------------- |
| status    | No       | Filter by order status |
| page      | No       | Page number            |
| size      | No       | Page size              |

### Example

```http
GET /orders?status=PENDING&page=0&size=20
```

## Success Response

### HTTP 200 OK

```json
{
  "content": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "amount": 100.50,
      "currency": "USD",
      "status": "PENDING"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

# 4. Update Order Status

Updates the current order status.

## Endpoint

```http
PATCH /orders/{id}/status
```

## Request Body

```json
{
  "status": "COMPLETED"
}
```

## Allowed Status Values

```text
PENDING
PROCESSING
COMPLETED
CANCELLED
```

## Success Response

### HTTP 200 OK

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "COMPLETED",
  "updatedAt": "2026-06-11T12:10:00Z"
}
```

## Invalid Transition

### HTTP 409 Conflict

```json
{
  "timestamp": "2026-06-11T12:10:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot transition COMPLETED -> PENDING",
  "path": "/orders/3fa85f64/status",
  "code": "INVALID_TRANSITION"
}
```

---

# Common Error Response

All API errors follow the same schema.

```json
{
  "timestamp": "2026-06-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/orders",
  "code": "VALIDATION_ERROR"
}
```

| Field     | Description            |
| --------- | ---------------------- |
| timestamp | Error occurrence time  |
| status    | HTTP status code       |
| error     | HTTP status text       |
| message   | Human-readable message |
| path      | API path               |
| code      | Application error code |

---

# Scheduler Behaviour

Orders created with:

```json
{
  "scheduledProcessingDelaySeconds": 60
}
```

are automatically promoted:

```text
PENDING → PROCESSING
```

after the configured delay.

---

# OpenAPI Documentation

Swagger UI:

```http
http://localhost:8080/swagger-ui/index.html
```

H2 Console:

```http
http://localhost:8080/h2-console
```


Add these tests under src/test/java: unit tests for services, MockMvc controller tests, and integration tests using SpringBootTest with embedded H2.
