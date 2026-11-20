# Performance Benchmarking Rules

1. Tests must not introduce arbitrary sleep delays to simulate wait times.
2. Read API endpoints must respond within 1500ms; transaction queries within 2000ms.
3. Persistent bottlenecks exceeding 3.0s in test suites must be investigated and remediated before merging.
