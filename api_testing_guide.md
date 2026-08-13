# Nexus API Master Testing Guide

This guide provides a comprehensive, step-by-step walkthrough for testing the Nexus API in production. It covers core workflows, edge cases, and security boundaries.

> [!TIP]
> **Prerequisites for Testing:**
> We highly recommend using a tool like **Postman** or **Insomnia** to run these tests, as they make it easy to manage JWT tokens and JSON payloads. Alternatively, you can use `curl` commands in your terminal.

---

## 1. Authentication & Identity

Nexus uses stateless JWT (JSON Web Tokens) for authentication. Every API request (except public endpoints) must include the `Authorization: Bearer <token>` header.

### 1.1 Acquiring a Token
- **Action:** Open your browser and navigate to `https://nexus-tep5.onrender.com/oauth2/authorization/google`
- **Expected:** You are redirected to Google. After logging in, you receive a JSON response containing your `token`.
- **Next Step:** Copy this `token`. In Postman, go to the "Authorization" tab, select "Bearer Token", and paste it.

### 1.2 Edge Case: Invalid or Expired Token
- **Action:** Make a request to *any* secure endpoint (e.g., `GET /api/v1/tickets`) with a modified, fake, or expired token.
- **Expected:** `401 Unauthorized` with an invalid token error.

---

## 2. Ticket Management Workflow

Tickets are the core entity. You will test the lifecycle of a ticket from creation to AI triage and resolution.

### 2.1 Create a Ticket
- **Method:** `POST /api/v1/tickets`
- **Headers:** `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Body:**
  ```json
  {
    "subject": "System is crashing on login",
    "description": "Whenever I try to log in, I get a 500 error."
  }
  ```
- **Expected:** `201 Created`. The response should include the `id`, `tenantId`, `status` (`NEW`), and an initial `version` of `0`.
- **Edge Case (Validation):** Send an empty `subject`. Expected: `400 Bad Request`.

### 2.2 Trigger AI Triage (The "Magic" Step)
- **Method:** `POST /api/v1/tickets/{ticket_id}/triage`
- **Headers:** `Authorization: Bearer <token>`
- **Expected:** `200 OK`. The AI should analyze the description and return the updated ticket. 
  - `status` should change to `CLASSIFIED` or `AI_DRAFTED`.
  - `category` and `priority` should be intelligently populated (e.g., `TECHNICAL`, `HIGH`).
  - `aiResponse` should contain a drafted reply to the customer.
- **Edge Case (Rate Limiting):** Hit this endpoint 20 times in 1 minute. Expected: `429 Too Many Requests` (Redis rate limiter kicks in).

### 2.3 Update Ticket (Optimistic Locking Test)
- **Method:** `PUT /api/v1/tickets/{ticket_id}`
- **Body:**
  ```json
  {
    "subject": "System is crashing on login (Investigating)",
    "status": "IN_PROGRESS",
    "version": 0
  }
  ```
- **Expected:** `200 OK`. The `version` in the response increments to `1`.
- **Edge Case (Concurrency):** Send the exact same request again with `"version": 0`.
- **Expected:** `409 Conflict`. This proves that two agents cannot silently overwrite each other's work!

---

## 3. Knowledge Base & RAG Workflow

The AI uses the Knowledge Base (PgVector) to accurately answer questions. 

### 3.1 Add a Knowledge Document
- **Method:** `POST /api/v1/knowledge`
- **Body:**
  ```json
  {
    "title": "Login Error 500 Fix",
    "content": "If a user gets a 500 error on login, it is usually because their billing is expired. Tell them to update their credit card."
  }
  ```
- **Expected:** `201 Created`. The backend silently contacts Gemini to generate a vector embedding and saves it to Postgres.

### 3.2 Test RAG (Retrieval-Augmented Generation)
- **Method:** Create a *new* ticket (`POST /api/v1/tickets`) with the subject "I got a 500 error logging in".
- **Method:** Trigger triage for this new ticket (`POST /api/v1/tickets/{new_ticket_id}/triage`).
- **Expected:** The `aiResponse` should specifically mention updating their credit card, proving that the AI successfully queried the vector database for your newly added document!

---

## 4. Multi-Tenant Security & RBAC (Row Level Security)

Nexus guarantees data isolation. Tenant A cannot see Tenant B's data, even if they guess the UUID.

### 4.1 Cross-Tenant Data Access (The Hacker Test)
To test this, you need a second tenant.
1. Temporarily run the seed script locally again (or ask me to write a quick SQL command) to generate a *second* user in a *different* tenant.
2. Get the `ticket_id` created by User A.
3. Log in as User B and acquire their JWT token.
4. **Action:** User B makes a request: `GET /api/v1/tickets/{ticket_id_from_user_a}`
5. **Expected:** `404 Not Found`. Even though the ticket exists in the database, Postgres Row Level Security completely hides it from User B.

### 4.2 Role-Based Access Control (RBAC)
Your seeded user is an `OWNER`. 
1. If you create a user with the `AGENT` role, they should be able to triage tickets.
2. **Edge Case:** An `AGENT` attempts to delete a Knowledge Base article (`DELETE /api/v1/knowledge/{id}`).
3. **Expected:** `403 Forbidden` (Only ADMIN or OWNER should manage knowledge base settings).

---

## 5. Resilience & Circuit Breakers

Nexus is built to survive third-party outages (like Groq or Gemini going down).

### 5.1 AI Fallback Test
- **Action:** Go to your Render Dashboard and temporarily change the `GROQ_API_KEY` to an invalid string like `fake-key`.
- **Wait:** Wait for Render to restart the app (about 3 mins).
- **Action:** Try to Triage a ticket (`POST /api/v1/tickets/{id}/triage`).
- **Expected:** The request should *not* crash with a 500. Instead, Resilience4J should catch the API failure and trigger the fallback method, returning a graceful `200 OK` with a message indicating AI triage is temporarily degraded or skipped.
