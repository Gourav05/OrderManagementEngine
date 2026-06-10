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
