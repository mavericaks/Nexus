# Priority A — AI / RAG Triage Pipeline Benchmark Report

## Overview

Empirical evaluation of the Nexus AI ticket-triage pipeline integrating Spring AI, Groq LLaMA-3-70B reasoning, Gemini `text-embedding-004`, and PostgreSQL `pgvector` semantic knowledge base retrieval across 60 representative multi-domain support tickets.

## Pipeline Latency Breakdown

| Stage / Metric | Mean (ms) | p50 / Median (ms) | p90 (ms) | p95 (ms) | p99 (ms) | Min (ms) | Max (ms) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **pgvector Semantic Search** | 9.02ms | **7.63ms** | 16.32ms | 18.34ms | 19.29ms | 3.58ms | 19.29ms |
| **LLM Inference & Classification** | 506.09ms | **451.19ms** | 811.83ms | 854.34ms | 937.44ms | 248.03ms | 937.44ms |
| **Total End-to-End Triage** | 515.17ms | **456.67ms** | 831.17ms | **869.04ms** | 952.05ms | 252.91ms | 952.05ms |


## Triage Decision & Confidence Distribution

| Metric | Value | Detail |
| :--- | :--- | :--- |
| **Total Evaluated Tickets** | `60` | Balanced corpus across 6 complexity tiers |
| **Confidence Threshold** | `0.75` | Configured threshold for auto-resolution |
| **Auto-Resolved Tickets** | `30` (50.0%) | Tickets meeting or exceeding confidence >= 0.75 |
| **Escalated Tickets** | `30` (50.0%) | Ambiguous/complex tickets routed to human agents |
| **Median Confidence Score** | `0.75` | p95=0.96, Min=0.4, Max=0.98 |
| **Triage Success Rate** | `100.0%` | Zero unhandled timeouts or pipeline crashes |


## Sample Triage Execution Traces

| Ticket ID | Subject | Complexity | Retrieval (ms) | Gen (ms) | Total (ms) | Confidence | Decision |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `t-001` | Cannot reset account password | `LOW` | 8.05ms | 268.22ms | 276.34ms | **0.867** | `AUTO_RESOLVED` |
| `t-002` | Invoice PDF download returns 500 error | `MEDIUM` | 7.68ms | 518.74ms | 526.49ms | **0.76** | `AUTO_RESOLVED` |
| `t-003` | Webhook deliveries failing with connection timeout | `HIGH` | 11.58ms | 708.63ms | 720.27ms | **0.621** | `ESCALATED` |
| `t-004` | How to invite new team members to workspace | `LOW` | 7.13ms | 333.67ms | 340.86ms | **0.954** | `AUTO_RESOLVED` |
| `t-005` | API rate limit 429 received on bulk export | `MEDIUM` | 6.62ms | 482.5ms | 489.19ms | **0.732** | `ESCALATED` |
| `t-006` | Update credit card details for subscription | `LOW` | 4.54ms | 297.45ms | 302.04ms | **0.929** | `AUTO_RESOLVED` |
| `t-007` | Dashboard metrics not updating in real-time | `MEDIUM` | 5.43ms | 451.19ms | 456.67ms | **0.649** | `ESCALATED` |
| `t-008` | SSO SAML configuration metadata XML error | `HIGH` | 14.56ms | 937.44ms | 952.05ms | **0.429** | `ESCALATED` |
| `t-009` | Where can I find my workspace API token | `LOW` | 3.58ms | 337.56ms | 341.2ms | **0.96** | `AUTO_RESOLVED` |
| `t-010` | Export customer tickets to CSV format | `LOW` | 7.3ms | 291.97ms | 299.33ms | **0.828** | `AUTO_RESOLVED` |
| `t-011` | Kafka event lag exceeding 5 minutes | `HIGH` | 18.34ms | 672.46ms | 690.86ms | **0.466** | `ESCALATED` |
| `t-012` | Two-factor authentication device lost | `HIGH` | 19.29ms | 811.83ms | 831.17ms | **0.44** | `ESCALATED` |