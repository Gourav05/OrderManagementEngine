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

# Order Status Lifecycle

```text
PENDING
   |
   v
PROCESSING
   |
   v
SHIPPED
   |
   v
DELIVERED

PENDING ---------> CANCELLED
PROCESSING ------> CANCELLED
```

## State Transition Rules

| Current Status | Allowed Next Status   |
| -------------- | --------------------- |
| PENDING        | PROCESSING, CANCELLED |
| PROCESSING     | SHIPPED, CANCELLED    |
| SHIPPED        | DELIVERED             |
| DELIVERED      | None                  |
| CANCELLED      | None                  |

### Notes

* Every order is created in `PENDING` state.
* `DELIVERED` and `CANCELLED` are terminal states.
* Updating an order to its current state is allowed.
* Invalid state transitions return HTTP 409 Conflict.

---

# Create Order

Creates a new order.

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
  "customerId": "CUST-1001",
  "amount": 1499.99,
  "currency": "INR",
  "scheduledProcessingDelaySeconds": 60,
  "metadata": {
    "channel": "WEB",
    "source": "PROMOTION"
  }
}
```

## Success Response

### HTTP 201 Created

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerId": "CUST-1001",
  "amount": 1499.99,
  "currency": "INR",
  "status": "PENDING",
  "createdAt": "2026-06-11T10:15:30Z",
  "updatedAt": "2026-06-11T10:15:30Z",
  "metadata": {
    "channel": "WEB",
    "source": "PROMOTION"
  }
}
```

## Validation Rules

| Field                           | Validation                |
| ------------------------------- | ------------------------- |
| amount                          | Must be greater than zero |
| currency                        | Cannot be blank           |
| customerId                      | Optional                  |
| scheduledProcessingDelaySeconds | Must be positive          |

## Error Response

### HTTP 400 Bad Request

```json
{
  "timestamp": "2026-06-11T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Amount must be greater than zero",
  "path": "/orders"
}
```

---

# Get Order By ID

Retrieves an existing order.

## Endpoint

```http
GET /orders/{orderId}
```

## Path Parameters

| Parameter | Type |
| --------- | ---- |
| orderId   | UUID |

## Success Response

### HTTP 200 OK

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerId": "CUST-1001",
  "amount": 1499.99,
  "currency": "INR",
  "status": "PROCESSING",
  "createdAt": "2026-06-11T10:15:30Z",
  "updatedAt": "2026-06-11T10:16:30Z"
}
```

## Not Found

### HTTP 404 Not Found

```json
{
  "timestamp": "2026-06-11T10:16:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found",
  "path": "/orders/123"
}
```

---

# Get Orders

Returns all orders with optional filtering.

## Endpoint

```http
GET /orders
```

## Query Parameters

| Parameter | Required | Description      |
| --------- | -------- | ---------------- |
| status    | No       | Filter by status |
| page      | No       | Page number      |
| size      | No       | Page size        |

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
      "amount": 1499.99,
      "currency": "INR",
      "status": "PENDING"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

# Update Order Status

Updates the status of an existing order.

## Endpoint

```http
PATCH /orders/{orderId}/status
```

## Request Body

```json
{
  "status": "SHIPPED"
}
```

## Allowed Status Values

```text
PENDING
PROCESSING
SHIPPED
DELIVERED
CANCELLED
```

## Success Response

### HTTP 200 OK

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "SHIPPED",
  "updatedAt": "2026-06-11T10:25:30Z"
}
```

---

# Invalid Transition Example

Attempting:

```text
PENDING -> DELIVERED
```

or

```text
SHIPPED -> PROCESSING
```

returns:

### HTTP 409 Conflict

```json
{
  "timestamp": "2026-06-11T10:25:30Z",
  "status": 409,
  "error": "Conflict",
  "message": "Invalid status transition from PENDING to DELIVERED",
  "path": "/orders/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
}
```

---

# State Transition Matrix

| From \ To  | PENDING | PROCESSING | SHIPPED | DELIVERED | CANCELLED |
| ---------- | ------- | ---------- | ------- | --------- | --------- |
| PENDING    | ✓       | ✓          | ✗       | ✗         | ✓         |
| PROCESSING | ✗       | ✓          | ✓       | ✗         | ✓         |
| SHIPPED    | ✗       | ✗          | ✓       | ✓         | ✗         |
| DELIVERED  | ✗       | ✗          | ✗       | ✓         | ✗         |
| CANCELLED  | ✗       | ✗          | ✗       | ✗         | ✓         |

---

# Scheduler Behavior

Orders created with:

```json
{
  "scheduledProcessingDelaySeconds": 60
}
```

are automatically promoted from:

```text
PENDING → PROCESSING
```

after the configured delay.

---

# Error Response Contract

All APIs return a consistent error structure.

```json
{
  "timestamp": "2026-06-11T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/orders"
}
```

| Field     | Description                |
| --------- | -------------------------- |
| timestamp | Error occurrence timestamp |
| status    | HTTP status code           |
| error     | HTTP status text           |
| message   | Human-readable error       |
| path      | API endpoint path          |

---

# API Documentation

Swagger UI:

```http
http://localhost:8080/swagger-ui/index.html
```

H2 Console:

```http
http://localhost:8080/h2-console
```



sts using SpringBootTest with embedded H2.
