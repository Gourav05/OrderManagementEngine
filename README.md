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

Create Order
------------
Endpoint: POST /orders
Headers: Content-Type: application/json

Request body (CreateOrderRequest):
- customerName: string (required, not blank)
- items: array of OrderItemRequest (required, not empty)
  - productName: string (required, not blank)
  - quantity: integer (required, min 1)
  - price: decimal (required, min 0.01)

Example request:
```json
{
  "customerName": "Alice",
  "items": [
    { "productName": "Widget A", "quantity": 2, "price": 499.99 },
    { "productName": "Widget B", "quantity": 1, "price": 299.50 }
  ]
}
```

Success (201 Created) — OrderResponse fields:
- id: string
- customerName: string
- status: OrderStatus (PENDING at creation)
- totalAmount: decimal (sum of item price*quantity)
- createdAt: timestamp (ISO-8601)
- items: array of OrderItemResponse
  - id: string
  - productName: string
  - quantity: integer
  - price: decimal

Example response:
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "customerName": "Alice",
  "status": "PENDING",
  "totalAmount": 1299.48,
  "createdAt": "2026-06-11T10:15:30Z",
  "items": [
    { "id": "it_1", "productName": "Widget A", "quantity": 2, "price": 499.99 },
    { "id": "it_2", "productName": "Widget B", "quantity": 1, "price": 299.50 }
  ]
}
```

Validation rules (from DTO annotations)
- customerName: @NotBlank (required)
- items: @NotEmpty, @Valid (at least one item)
- item.productName: @NotBlank
- item.quantity: @NotNull, @Min(1)
- item.price: @NotNull, @DecimalMin("0.01")

Errors (ErrorResponse)
All error responses use ErrorResponse:
- timestamp: ISO-8601 instant
- status: HTTP status code
- error: HTTP status text
- message: human-readable message
- path: API path
- code: optional machine-readable error code

Example validation error (400):
```json
{
  "timestamp": "2026-06-11T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "items must not be empty",
  "path": "/orders",
  "code": "VALIDATION_ERROR"
}
```

Get Order by ID
----------------
Endpoint: GET /orders/{orderId}
Path parameter: orderId (string/UUID)

Success (200 OK) returns OrderResponse (see fields above)
Not found (404) returns ErrorResponse with status 404 and message "Order not found".

List Orders
-----------
Endpoint: GET /orders
Query parameters:
- status: optional (filter by OrderStatus)
- page: optional (defaults to 0)
- size: optional (defaults to 20)

Response (200 OK): paginated list with fields: content (OrderResponse[]), page, size, totalElements

Update Order Status
-------------------
Endpoint: PATCH /orders/{orderId}/status
Request body (UpdateOrderStatusRequest):
```json
{ "status": "SHIPPED" }
```
Allowed values: PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED

Success (200 OK): returns updated OrderResponse (or a minimal object with id, status, updatedAt)
Invalid transition: HTTP 409 Conflict with ErrorResponse

State transition rules (implemented in service layer)
- PENDING -> PROCESSING, CANCELLED
- PROCESSING -> SHIPPED, CANCELLED
- SHIPPED -> DELIVERED
- DELIVERED: terminal
- CANCELLED: terminal

Scheduler behavior
- If scheduled promotions are implemented, they should be documented here. (Current DTOs do not include a scheduled delay field.)

Developer notes
---------------
- DTOs live at Order-engine/src/main/java/com/peerisland/orderengine/dto
- OrderStatus enum: com.peerisland.orderengine.domain.OrderStatus
- Validation uses Jakarta Bean Validation annotations; controllers should annotate @Valid and return ErrorResponse on ConstraintViolationException.
