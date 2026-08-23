/**
 * k6 Load Test: Circuit Breaker Resilience Demo
 *
 * Demonstrates Nexus's Resilience4j circuit breaker protecting the
 * triage pipeline from cascading failures when the LLM (Groq) is
 * slow or unavailable.
 *
 * Usage:
 *   k6 run --env BASE_URL=http://localhost:8080 \
 *          --env TENANT_ID=aaaa0000-0000-0000-0000-000000000001 \
 *          --env JWT_TOKEN=<your-jwt-token> \
 *          docs/load-tests/circuit_breaker_demo.js
 *
 * What it tests:
 *   1. Normal triage flow under moderate load (10 VUs)
 *   2. Burst load (50 VUs) to trigger circuit breaker OPEN state
 *   3. Recovery period to observe HALF_OPEN → CLOSED transition
 *
 * Expected behavior:
 *   - Phase 1: All requests succeed (circuit CLOSED)
 *   - Phase 2: As LLM latency increases, ~50% fail → circuit OPEN
 *     → subsequent requests fail immediately (no LLM call)
 *   - Phase 3: After 30s wait, circuit goes HALF_OPEN, allows 3 probe
 *     requests → if they succeed, circuit CLOSED again
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const triageSuccess = new Rate('triage_success');
const triageDuration = new Trend('triage_duration_ms');
const circuitBreakerOpen = new Rate('circuit_breaker_open');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TENANT_ID = __ENV.TENANT_ID || 'aaaa0000-0000-0000-0000-000000000001';
const JWT_TOKEN = __ENV.JWT_TOKEN || '';

// 3-phase scenario: normal → burst → recovery
export const options = {
  scenarios: {
    // Phase 1: Normal load (10 VUs for 30s)
    normal_load: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
      startTime: '0s',
      tags: { phase: 'normal' },
    },
    // Phase 2: Burst load (50 VUs for 20s) — should trigger circuit breaker
    burst_load: {
      executor: 'constant-vus',
      vus: 50,
      duration: '20s',
      startTime: '30s',
      tags: { phase: 'burst' },
    },
    // Phase 3: Recovery (10 VUs for 40s) — circuit should recover
    recovery: {
      executor: 'constant-vus',
      vus: 10,
      duration: '40s',
      startTime: '50s',
      tags: { phase: 'recovery' },
    },
  },
  thresholds: {
    // Overall success rate should be > 60% (circuit breaker will reject some)
    'triage_success': ['rate>0.6'],
    // P95 latency should be < 15s (circuit breaker fast-fails)
    'triage_duration_ms{phase:normal}': ['p(95)<15000'],
    // During burst, fast-fail should be < 1s
    'triage_duration_ms{phase:burst}': ['p(50)<1000'],
  },
};

export default function () {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${JWT_TOKEN}`,
  };

  // Step 1: Create a ticket
  const createPayload = JSON.stringify({
    subject: `Load test ticket ${Date.now()}`,
    description: 'This is an automated load test ticket to verify circuit breaker behavior under high concurrency. The AI triage pipeline should handle this gracefully even when the LLM service is under pressure.',
  });

  const createRes = http.post(
    `${BASE_URL}/api/v1/tenants/${TENANT_ID}/tickets`,
    createPayload,
    { headers, tags: { name: 'create_ticket' } }
  );

  if (createRes.status !== 201 && createRes.status !== 200) {
    triageSuccess.add(false);
    return;
  }

  const ticket = JSON.parse(createRes.body);
  const ticketId = ticket.id;

  // Step 2: Trigger triage on the ticket
  const triageStart = Date.now();
  const triageRes = http.post(
    `${BASE_URL}/api/v1/tenants/${TENANT_ID}/tickets/${ticketId}/triage`,
    null,
    { headers, tags: { name: 'triage_ticket' }, timeout: '30s' }
  );
  const triageEnd = Date.now();

  triageDuration.add(triageEnd - triageStart);

  const success = triageRes.status === 200;
  triageSuccess.add(success);

  // Track circuit breaker opens (503 = service unavailable = circuit open)
  if (triageRes.status === 503 || triageRes.status === 429) {
    circuitBreakerOpen.add(true);
  } else {
    circuitBreakerOpen.add(false);
  }

  check(triageRes, {
    'triage returned 200': (r) => r.status === 200,
    'response has category': (r) => {
      if (r.status !== 200) return true; // skip for failed requests
      const body = JSON.parse(r.body);
      return body.category !== undefined;
    },
  });

  sleep(0.5);
}

export function handleSummary(data) {
  const phases = ['normal', 'burst', 'recovery'];
  let summary = '\n╔══════════════════════════════════════════════════════════╗\n';
  summary += '║        CIRCUIT BREAKER RESILIENCE DEMO RESULTS          ║\n';
  summary += '╠══════════════════════════════════════════════════════════╣\n';

  const successRate = data.metrics.triage_success ? 
    (data.metrics.triage_success.values.rate * 100).toFixed(1) : 'N/A';
  const cbOpenRate = data.metrics.circuit_breaker_open ?
    (data.metrics.circuit_breaker_open.values.rate * 100).toFixed(1) : 'N/A';

  summary += `║  Overall Triage Success Rate:  ${successRate}%\n`;
  summary += `║  Circuit Breaker Open Rate:    ${cbOpenRate}%\n`;
  summary += '╚══════════════════════════════════════════════════════════╝\n';

  return {
    stdout: summary,
  };
}
