---
name: performance-profiler
description: Analyzes execution timing logs, profiles latency distributions (p50, p90, p95), flags execution bottlenecks, and provides optimization recommendations.
---

# Performance Benchmarker Skill

This skill automates performance profiling of test suites and application response times using `ExecutionTimingListener` CSV telemetry.

---

## 1. Benchmarking & Bottleneck Analysis Protocol

1. **Trigger Suite Execution**: Run tests with `ExecutionTimingListener` enabled (active in all standard TestNG suites).
2. **Execute Profiling Script**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/analyze-performance-bottlenecks.ps1 -BottleneckThresholdSeconds 3.0
   ```
3. **Analyze Metric Distributions**:
   - **p50 (Median)**: Baseline expected latency.
   - **p90 / p95**: Tail latency revealing resource contention or network spikes.
   - **Max**: Worst-case outlier step.
4. **Actionable Remediation**:
   - If UI step &ge; 3.0s: Review locator strategy (replace XPath with ID) or check for unnecessary wait timeouts.
   - If API call &ge; 1.5s: Check database query execution plan and connection pool saturation.
