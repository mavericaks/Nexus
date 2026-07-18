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
-
