# Kafka Producer & Consumer with Spring Boot (Advanced Kafka Patterns)

## Overview
This project demonstrates **production-grade event-driven architecture** with Apache Kafka using Spring Boot.  
It covers:

---

## Tech Stack
- Java 17+
- Spring Boot + Spring Kafka + Spring Data JPA
- PostgreSQL
- Docker / Docker Compose
- Maven
- JUnit 5 + Mockito for tests

---

- Kafka Producer + Consumer (sync/async)
- Guaranteed delivery with retries + DLT
- Idempotent Producer & Consumer
- **Transactional Messaging** with DB integration
- **Kafka Testing: Unit + Integration**
- Distributed workflows using **Saga**
  - Choreography-based Saga
  - Orchestration-based Saga
  - Compensating transactions
- Dockerized **Kafka cluster setup** using KRaft (no Zookeeper)
---

## Architecture

### Microservices:
| Service | Description |
|--------|-------------|
| Producer | REST → Kafka topic, acks, retries, idempotence |
| Consumer | Transactional event processing + idempotency |
| Order Service | Sends order events (start of Saga) |
| Inventory Service | Reserves product for order |
| Payment Service | Handles payment processing |
| History DB | Stores processed event + order status |

### Kafka Topics (examples)
| Topic | Purpose |
|-------|---------|
| `order-created` | Order saga start event |
| `reserve-product` | Command to Inventory service |
| `product-reserved` | Inventory success |
| `product-reservation-failed` | Inventory fail |
| `process-payment` | Payment command |
| `payment-processed` | Success |
| `payment-failed` | Failure |
| `approve-order` | Final approval |
| `reject-order` | Final rejection |
| `*-dlt` | Dead letter topics for failures |

---

## Key Features

### Producer Capabilities
- Async + Sync sends
- Callback metadata logging
- Configured reliability:
  - `acks=all`, `min.insync.replicas`
  - Retries + Backoff
  - **Idempotent Producer**
- Custom KafkaTemplate + ProducerFactory

### Consumer Capabilities
- `@KafkaListener`, `@KafkaHandler`
- Error-handling:
  - `ErrorHandlingDeserializer`
  - `DefaultErrorHandler`
  - **DeadLetterPublishingRecoverer**
- Retry only for retryable exceptions
- **Idempotency** using unique message ID checks via DB
- **Transactions**:
  - Kafka + DB synchronized commit
  - `read_committed` message isolation

---

## Kafka + Database Transactions

### Implementations
- `JpaTransactionManager` for DB
- `KafkaTransactionManager` for Kafka
- **ChainedTransactionManager** for atomic operations:
  - Message consumed → DB updated → offsets committed  
    (all succeed or all rolled back)

- Configured logging for both transaction managers

Benefits:  
✔ Avoids **duplicate processing**  
✔ Prevents **message loss** during rollback  

---

## Saga Pattern Implementation

### Supported Patterns
| Pattern | Description |
|--------|-------------|
| Choreography Saga | Services react to events + publish next event |
| Orchestration Saga | Dedicated orchestrator commands services |
| Compensating Actions | Reverse previous success when step fails |

### Happy Path Flow (simplified)

`OrderCreatedEvent` → Reserve stock → Payment → Approve order

### Failure / Compensation Flow
If payment fails → Cancel reservation → Reject order

This guarantees **eventual consistency** across microservices.

---

## Testing Strategy

| Testing Type | Coverage |
|-------------|----------|
| Unit Tests | Producer/Consumer logic, config checks |
| Integration Tests | Kafka + DB + transaction boundaries |
| Message Assertions | Metadata, headers, idempotency |
| Arrange → Act → Assert pattern | Enforced consistently |

Key Test Items:
- Verify idempotent producer settings
- Integration tests with embedded Kafka
- Setup/teardown for clean consumer state


## Kafka + Docker Setup

This project uses **Kafka KRaft mode** — modern Kafka without Zookeeper.

### Key Topics Covered
- Docker installation & prerequisites
- Kafka Docker image details
- Kafka listener configuration
- External tool access from host machine
- Persistent storage using **Docker volumes**
- Running **multiple brokers** for a Kafka cluster
- Kafka CLI execution:
  - Inside the running container
  - From host machine (mapped scripts)


---
