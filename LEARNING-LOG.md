# Learning Log

One entry per unit (see playbook §2.11), written by me, in my own words, after the checkpoint — not copied from the agent's explanation. The point of this file is producing the recall myself, not having a record of what was said.

## Phase 0
Unit I
-In this phase we started this project with instructing the agent to read all the docs and understand the project structure.
-The setup checklist was documented to keep the required components and credentials for the project.
Unit II :
-Here we declared a parent POM which consists of packaging dependencyManagement and why not single POM for each modules .
Unit III :
-Created child POM which actual tells what it needs as parent POM locked in on versions , it also fixed the scope of postgres as runtime to avoid raw bypass and test to not include them in JAR.
Unit IV :
-Created the main directory for the application and the main class which is the entry point for the application . NexusApplication.java and application.yml .
Unit V :
-Created the docker-compose.yml file to run postgres+pgvector , redis and kafka as 3 different containers with named volumes for persistant data and health checks are defined to actually ensure the containers are up and running. Also create .env example file .
Unit VI :
-Created local profile of dev with a separate application-dev.yml file. There are some shared config in the parent application.yml and dev specific config such as flyway clean and sql format required for dev only . 
Unit VII :
-Created ArchUnit Test to ensure domain purity i.e., no spring or jpa imports in domain classes. This was done to keep the code clean and fast to test. This is done only for prod code not for test as for test SpringBootTest is required .
Unit VIII :
-Created github ci.yml file to keep a check on the code and files getting commited to the repo so that no sensitive data is commited to main branch , no STUB's are left and a complete execution of the code in the fresh environment .

## Phase 1
Unit I :
-Created Domain Value Objects which is pure java no JPA entity and these are enums and value object because every other layer depends on this so defining them first with flyway means we can write state machine transition and then build database schema that stores them . 8 state machine lifecycle(NEW , CLASSIFIED , RESOLVED ,CLOSED ,ETC) with 4 levels of priority(LOW , MEDIUM , HIGH , CRITICAL) and 5 categories of tickets are created(BILLING , TECHNICAL , ETC) 
Unit II :
-Created Flyway Baseline Migration (V1__baseline_schema.sql) which creates the tenants and tickets tables . Used Flyway because schema change is version controlled and we can redeploy old version if needed . The baseline migration is used to create the schema that is already present in the database . Using ddl_auto : validate because hibernate shouldn't change the db for any typo .
Unit III :
-Created JPA Entities which is the bridge between domain and database . These entities are annotated with @Entity , @Table , @Column and @Version to tell Hibernate how to map objects to rows . These entities are in infrastructure.persistence package and not in domain package because they carry framework imports . @Version is used for optimistic locking and @Enumerated(EnumType.STRING) is used to store the enum values as strings in the database .
Unit IV :
-Created the core of the multi-tenancy security i.e., Row Level Security in Postgres to ensure cross-tenant data isolation without any code changes . Created tenant role with least privileges and nexus_app with CRUD and session context is set by setcontext() . This RLS is the single source of truth for data isolation . nexus_app role is RLS filtered and nexus is owner i.e., superuser .USING is used as READ Filter i.e., automatic WHERE clause and WITH CHECK is used as WRITE Filter i.e., automatic INSERT and UPDATE validation .
Unit V :
-Created the Ticket State Machine which is a pure java utility class that enforces the ticket lifecycle. It has static methods that check for valid transitions and return appropriate error messages. This is done to keep the code clean and fast to test.EnumMap is used to reduce the overhead of hashing in the hashmap .
Unit VI :
-Phase 1 Gate pass and verfication with Merge Pull Request for first milestone in Nexus Product v1.0.0

Phase 2 : 
Unit I :
-We added thereal multitenancy by using TenantResolver which tells Spring Boot Multitenancy which tenant to use for the current request and TenantContext which stores the tenant id in a threadlocal and SessionScoped to share the tenant id across the request. Also added TenantContext to application.yml with 'no-tenant' default value.
Unit II :
-Created DTOs and validation using @NotBlank and @Size to validate the incoming requests and also created a manual mapper to map the DTOs to Entities and vice versa. Used java record which is immutable data carriers no setters and separate TransitionTicketRequest from UpdateTicketRequest to avoid lazy loaded leak from entity to API layer.
Unit III :
-Created global exception handling because Without a global handler, every controller method needs its own try/catch, and error responses look different everywhere. @ControllerAdvice centralizes error handling — one class maps each exception type to the correct HTTP status code and a consistent JSON error shape.
Custom exceptions carry specific context (which tenant? which ticket? what transition was attempted?) so the error response tells the client exactly what went wrong, not just "500 Internal Server Error."
@RestControllerAdvice = @ControllerAdvice + @ResponseBody. It intercepts exceptions from ALL controllers and returns JSON.
Why custom exceptions instead of generic RuntimeException? — Each carries specific context (which tenant? which ticket? what transition?). The handler can then produce precise error messages.
Why 409 Conflict for transitions? — 400 means "bad request format." 409 means "valid request but conflicts with the current resource state" — semantically more accurate for "you can't close a ticket that's already closed."
Unit IV :
-Created JPA Repositories for basic CRUD operations + specification executor for dynamic queries. No WHERE clause with tenant_id as RLS handles it at Postgres level . JpaSpecificationExecutor enables dynamic queries based on the filters passed in the request. JpaSpecificationExecutor is a Spring Data JPA interface that allows you to execute dynamic queries based on the filters passed in the request.
Unit V :
-Created the Service Layer + Unit Tests . The service layer is where business logic lives. It orchestrates: validate tenant exists → create/update/transition entity → use state machine → persist → return DTO. Controllers call services, services call repositories. Services are @Transactional so the entire operation succeeds or fails atomically — and that's what triggers our TenantAwareDataSource to inject SET LOCAL.
Unit VI :
-Created The controller is the thin HTTP layer — it receives requests, validates input (@Valid), delegates to the service, and returns the response. Controllers should be dumb: no business logic, no direct repository calls. Just HTTP mapping.
HTTP request
  → TenantContextFilter (extracts tenantId, sets ThreadLocal)
    → TicketController (validates @Valid, delegates to service)
      → TicketService (@Transactional → SET LOCAL fires)
        → TicketRepository (RLS filters queries)
          → Postgres (returns only current tenant's rows)
        → TicketMapper (entity → DTO)
      → GlobalExceptionHandler (catches errors → correct HTTP status)
    → JSON response
Unit VII :
-E2E test results (app booted against Docker Compose)
| Test | Result | Detail |
|---|---|---|
| POST create (Acme) | ✅ 201 | Returned ticket with status=NEW, priority=HIGH, category=TECHNICAL |
| POST create (Beta) | ✅ 201 | Different tenant, separate ticket |
| GET list (Acme) | ✅ 200 | Only Acme tickets returned (3), no Beta leakage — **RLS proven** |
| PATCH transition (NEW→CLASSIFIED) | ✅ 200 | Status changed, updatedAt advanced |
| PATCH illegal (CLASSIFIED→CLOSED) | ✅ 409 | State machine rejected it |
| PUT update | ✅ 200 | Subject + priority changed, version incremented to 1 |
| POST validation (empty subject) | ✅ 400 | Bean Validation rejected blank subject |
| DELETE | ✅ 204 | Ticket gone, subsequent GET returns 404 |

Phase 3 :

Unit I :
-Created SecurityConfig.java to centralize security configuration. Disables CSRF and configures stateless sessions. Configures JWT decoder using HMAC-SHA256. Sets up BCrypt password encoder. Enables method-level security.

Unit II :
-Created JWT Token Provider which generates signed JWT tokens for authenticated users.
-Created Auth Controller which handles email/password login requests and returns JWT tokens.
-Created NexusUserDetails which is a custom UserDetails implementation.
-Created LoginRequest and LoginResponse DTOs.
-Created GlobalExceptionHandler which handles BadCredentialsException and AccessDeniedException.

Unit III :
-Key learning: The RLS vs Login chicken-and-egg problem — during login you don't have a tenant context, so RLS blocks user lookup. Solution: a secondary DataSource running as DB owner, used only for the login query.
-Created OAuth2LoginSuccessHandler.java — handles Google auth callback, issues our JWT
-Created application-dev.yml — Google OAuth2 client-id/secret config
-Created SecurityConfig.java — added oauth2Login() with custom success handler
-Created application.yml (test) — dummy OAuth2 config so tests don't fail

#### The OAuth2 → JWT token exchange flow
```
User → GET /oauth2/authorization/google
     → Google login page
     → Google redirects to /login/oauth2/code/google
     → Spring exchanges auth code for Google tokens + user info (OIDC)
     → OAuth2LoginSuccessHandler:
         1. Extract Google email from OidcUser
         2. Look up email in our users table (bypass RLS via authDataSource)
         3. If user exists → issue our JWT with tenantId + roles
         4. If not → 403 "No Nexus account linked to this email"
     → Return JWT as JSON
```

#### Why NOT auto-create users on Google login?
In B2B SaaS, tenant admins control who has access. A random Google user shouldn't get an account just by clicking "Sign in with Google." The admin pre-creates users (with email + role), then the user can sign in via Google. This is the "account linking" pattern.

#### Why IF_REQUIRED sessions instead of STATELESS?

The OAuth2 redirect flow needs temporary session state — Google sends the user to a consent page and back. Spring stores a "state" parameter in the session to validate the callback isn't forged (CSRF protection for OAuth). API requests still use JWT (no sessions).

Unit IV :
-