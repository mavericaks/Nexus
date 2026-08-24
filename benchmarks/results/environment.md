# Benchmark Environment Specification

This document records the exact physical and software environment used to execute the Nexus benchmark suite, ensuring reproducibility and empirical context.

## Hardware Specifications

| Component | Specification | Notes |
| :--- | :--- | :--- |
| **CPU Model** | 11th Gen Intel(R) Core(TM) i5-11400H @ 2.70GHz (Boost to 4.50GHz) | High-performance mobile x86_64 architecture |
| **Cores / Threads** | 6 Physical Cores / 12 Logical Processors | Hyper-Threading enabled |
| **System Memory (RAM)**| 16.0 GB DDR4 (16,406,512 KB visible) | Available Free RAM during test: ~6.5 GB |
| **Storage Subsystem** | NVMe PCIe M.2 SSD | High-throughput low-latency I/O |
| **Host Machine Type** | Bare-metal Local Workstation (Non-virtualized Host) | Direct hardware access |

## Operating System & Runtimes

| Component | Version / Release | Details |
| :--- | :--- | :--- |
| **Operating System** | Microsoft Windows 11 Home Single Language | Version 10.0.26200, 64-bit OS |
| **Java Development Kit (JDK)** | OpenJDK / Oracle JDK 21.0.11+9-LTS-211 (64-Bit Server VM) | Mixed mode, tiered compilation, ZGC/G1GC support |
| **Build Automation** | Apache Maven 3.9.11 | Embedded Maven Wrapper (`mvnw.cmd`) |
| **Node.js / Frontend Runtime** | Node.js v20+ / Next.js 16.3.1 (Turbopack) | Frontend execution runtime |
| **Python Runtime** | Python 3.14.0 (amd64) | Benchmark orchestration, client concurrency & statistics engine |

## Infrastructure & Service Versions

| Service | Version | Deployment Topology | Connection / Port Details |
| :--- | :--- | :--- | :--- |
| **PostgreSQL (Primary DB)** | PostgreSQL 16.4 / Neon Serverless Postgres with `pgvector` 0.7.0 | Cloud Neon / Managed Instance + Local Container Config | High-port mapping `15432:5432` / SSL enabled |
| **Vector Search Extension** | `pgvector` v0.7.0 (`vector(1536)` & `vector(768)`) | Integrated with PostgreSQL 16 | HNSW / IVFFlat indexing & Cosine distance `<=>` |
| **Redis** | Redis 7.2.x Alpine | Local Container Topology / Embedded in-memory | High-port mapping `16379:6379` |
| **Apache Kafka** | Apache Kafka 3.8.0 / 3.8.1 (KRaft mode / EmbeddedKafka) | KRaft single-broker dev cluster & Spring EmbeddedKafka | High-port mapping `19092:9092` / dynamic in-memory test broker |
| **Prometheus** | Prometheus v2.53.0 | Containerized Metric Scraper | Port `19090:9090` (Scraping `/actuator/prometheus`) |
| **Grafana** | Grafana v11.1.0 | Visual Dashboard | Port `13000:3000` |

## Application & Repository State

| Attribute | State |
| :--- | :--- |
| **Git Branch** | `main` |
| **Git Commit Hash** | `fa24208cf7d461a1926cf6ad4fb7cb8cb6650e85` |
| **Spring Boot Version** | 3.4.1 |
| **Spring AI Version** | 1.0.9 (OpenAI/Groq starter + Google Gemini embedding) |
| **Spring Modulith Version** | 1.3.1 |
| **Resilience4j Version** | 2.2.0 |
| **Test Execution Mode** | Local Native JDK 21 process with embedded Kafka/H2/Postgres & HTTP concurrency engines |
