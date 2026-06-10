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
- OpenAPI UI (springdoc) is available when the app is running (look for /swagger-ui/index.html).
- Scheduler delay and other operational settings are externalized via configuration. See src/main/resources/application*.yml for defaults.

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

Add these tests under src/test/java: unit tests for services, MockMvc controller tests, and integration tests using SpringBootTest with embedded H2.

API Test Contracts

CreateOrderRequest (POST /orders)
- Content-Type: application/json
- Body schema: { "customerId": "UUID?", "amount": number, "currency": string, "metadata": object?, "scheduledProcessingDelaySeconds": integer? }
- Success: 201 Created
- Response body (OrderResponse): { "id": "UUID", "customerId": "UUID?", "amount": number, "currency": string, "status": "PENDING|PROCESSING|COMPLETED|CANCELLED", "createdAt": "ISO8601", "updatedAt": "ISO8601", "metadata": object? }

UpdateStatusRequest (PATCH /orders/{id}/status)
- Content-Type: application/json
- Body schema: { "status": "PENDING|PROCESSING|COMPLETED|CANCELLED" }
- Success: 200 OK with OrderResponse
- Invalid transition: 409 Conflict with ErrorResponse

GET /orders and GET /orders/{id}
- GET /orders?status=STATUS&page=0&size=20 -> 200 OK; response: { "content": [OrderResponse], "page": number, "size": number, "totalElements": number }
- GET /orders/{id} -> 200 OK or 404 Not Found

ErrorResponse schema
- { "timestamp": "ISO8601", "status": number, "error": "string", "message": "string", "path": "string", "code": "string" }

Examples
- Create request:
  { "amount": 100.0, "currency": "USD" }
- Create response:
  { "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6", "amount": 100.0, "currency": "USD", "status": "PENDING", "createdAt": "2026-06-11T00:00:00Z", "updatedAt": "2026-06-11T00:00:00Z" }
- Error response example (invalid transition):
  { "timestamp": "2026-06-11T00:01:00Z", "status": 409, "error": "Conflict", "message": "Cannot transition COMPLETED -> PENDING", "path": "/orders/...,/status", "code": "INVALID_TRANSITION" }
