# CLAUDE.md - AI Agent Guidelines

Create high-quality software following Vaughn Vernon's Domain-Driven Design principles, with strong emphasis on event sourcing and event-driven programming. Generate clean, modular code embodying DDD concepts (bounded contexts, aggregates, domain events, entities, value objects), while adhering to event sourcing for state management.

## Core Guidelines:

1. **Domain-Driven Design**: Center design around ubiquitous language; define clear bounded contexts; design aggregates as consistency boundaries; use domain events for state changes; provide repository patterns.

2. **Event Sourcing**: Represent state as sequence of immutable events; enable state reconstruction from event history; design self-contained events; use append-only storage.

3. **Event-Driven Programming**: Decouple components using publish/subscribe patterns; define clear patterns for emitting/handling events; support eventual consistency.

4. **Security**: Encrypt data at rest and in transit; minimize exposure of sensitive data; incorporate access control; include auditability.

5. **Strong Typing**: Use explicit types for domain models; define distinct types for entities/values; avoid primitive obsession; maintain type consistency across system.

6. **Error Handling**: Define domain-specific exceptions; categorize errors (recoverable/non-recoverable); enforce invariants; include meaningful messages.

7. **Architecture**: Use layered architecture (domain, application, infrastructure); apply OO patterns; ensure immutability; write expressive code reflecting ubiquitous language.

## Commands:
- Build: `mvn clean install`
- Run tests: `mvn test`
- Single test: `mvn test -Dtest=TestClassName#testMethodName`
- Style check: `mvn checkstyle:check`