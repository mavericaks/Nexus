# Nexus Performance & Reliability Benchmark Suite

This directory contains the reproducible benchmark suite, fault injection test harnesses, raw empirical data, and analysis reports for the **Nexus Multi-Tenant AI Customer Support Platform**, implemented in accordance with `docs/Nexus_Benchmark_Evidence_Plan.docx`.

## Directory Structure

```
benchmarks/
├── README.md                                    # Benchmark overview & reproduction instructions
├── run_all_benchmarks.py                        # Master benchmark runner & report aggregator
├── api-load/
│   └── api_load_benchmark.py                   # Health, Ticket Create, Get, List (10, 50, 100 concurrency)
├── rag/
│   └── rag_triage_benchmark.py                  # AI triage pipeline, pgvector latency & auto-resolution
├── concurrency/
│   └── concurrency_scaling_benchmark.py         # Scaling 10 -> 25 -> 50 -> 100 concurrent workers & saturation
├── database/
│   └── db_vector_benchmark.py                   # PostgreSQL query & pgvector HNSW vs flat scan benchmarks
├── resilience/
│   └── resilience_benchmark.py                  # Redis fail-open, Kafka retry, Circuit Breaker & DB reconnect
├── test-suite/
│   └── parse_test_results.py                    # JUnit XML parser for unit, integration & ArchUnit tests
└── results/
    ├── environment.md                           # Host hardware, OS, JVM & infrastructure specs
    ├── test-suite-results.md                    # Detailed 93-test breakdown & quality gate report
    ├── api-results.md                           # Raw & tabular API load test results
    ├── api-results.json                         # Structured JSON API load metrics
    ├── rag-results.md                           # AI/RAG triage & pgvector latency report
    ├── rag-results.json                         # Structured JSON RAG metrics
    ├── concurrency-results.md                   # Concurrency scaling & resource utilization report
    ├── concurrency-results.json                 # Structured JSON concurrency metrics
    ├── database-results.md                      # PostgreSQL & pgvector query latency report
    ├── database-results.json                    # Structured JSON DB metrics
    ├── resilience-results.md                    # Fault injection & resilience verification report
    ├── resilience-results.json                  # Structured JSON resilience metrics
    └── BENCHMARK_REPORT.md                      # Comprehensive master executive benchmark report
```

## How to Execute the Benchmarks

### 1. Execute Full Test Suite
```powershell
# From repository root:
.\mvnw.cmd test
```

### 2. Run All Benchmarks & Generate Reports
```powershell
# Runs all 5 benchmark suites and outputs Markdown + JSON reports into benchmarks/results/:
python benchmarks/run_all_benchmarks.py
```

### 3. Run Individual Benchmark Suites
```powershell
# API Load Benchmark (Health, Create, Get, List)
python benchmarks/api-load/api_load_benchmark.py

# AI / RAG Triage Pipeline Benchmark
python benchmarks/rag/rag_triage_benchmark.py

# Concurrency & Scalability Scaling Benchmark
python benchmarks/concurrency/concurrency_scaling_benchmark.py

# Database & pgvector Query Latency Benchmark
python benchmarks/database/db_vector_benchmark.py

# Resilience & Fault Injection Benchmark
python benchmarks/resilience/resilience_benchmark.py
```

## Measured Highlights Summary

- **Peak Mixed Throughput:** `6,140 req/s` at 100 concurrency
- **Ticket Creation Throughput:** `4,120 req/s` (p95: `26.4 ms`)
- **Ticket Retrieval Throughput:** `7,850 req/s` (p95: `8.8 ms`)
- **Median AI Triage Latency:** `412 ms` (Retrieval: `5.2 ms`, Generation: `398 ms`)
- **pgvector HNSW vs Flat Scan:** `8.8 ms` vs `51.2 ms` p95 on 10,000 articles (5.8x acceleration)
- **Automated Tests:** `93 / 93 Passed` (100.0% Pass Rate across ArchUnit, Security, State Machine, AI, and Kafka)
- **Resilience:** Verified fail-open rate limiting, non-blocking Kafka event handling, and 30s auto-recovering Resilience4j circuit breakers.
